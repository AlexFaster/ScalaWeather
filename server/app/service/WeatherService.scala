package service

import java.time.temporal.ChronoUnit
import java.time.{LocalDateTime, ZoneOffset}
import javax.inject.Inject

import exceptions.BadRequestToWeatherService
import play.api.Configuration
import play.api.libs.json.JsArray
import play.api.libs.ws.{WSClient, WSResponse}
import shared.model.{WeatherInfoDTO, WeatherInfoHistoricalDTO, WeatherInfoListDTO, WindInfoDTO}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

// right way is to create a model and then create dto mapper, but it is redundant here

class WeatherService @Inject()(
                                ws: WSClient,
                                config: Configuration
                              ) {

  import WeatherService._

  def getCurrentWeather(lat: Double, lon: Double): Future[WeatherInfoDTO] = {
    ws.url(CURRENT_WEATHER_URL)
      .addHttpHeaders("Accept" -> "application/json")
      .addQueryStringParameters("lat" -> lat.toString)
      .addQueryStringParameters("lon" -> lon.toString)
      .addQueryStringParameters("appid" -> config.get[String](API_KEY_PATH))
      .withRequestTimeout(10000.millis)
      .get()
      .map(response => getCurrentWeatherFromResponse(response))

  }

  private def getCurrentWeatherFromResponse(response: WSResponse): WeatherInfoDTO = {
    val respStatusCode = response.json \ "cod"
    if (respStatusCode.as[Int] == 200) {
      WeatherInfoDTO(
        Option.empty,
        (response.json \ "main" \ "temp").as[Double],
        (response.json \ "main" \ "temp_min").as[Double],
        (response.json \ "main" \ "temp_max").as[Double],
        (response.json \ "weather" \\ "main").map(_.as[String]),
        Option(WindInfoDTO(
          (response.json \ "wind" \ "speed").asOpt[Double],
          (response.json \ "wind" \ "deg").asOpt[Double]
        )),
        (response.json \ "rain").asOpt[String]
      )
    } else {
      throw new BadRequestToWeatherService("Failed request to weather app")
    }

  }

  def getLatestWeather(lat: Double, lon: Double): Future[WeatherInfoListDTO] = {
    ws.url(LATEST_WEATHER_URL)
      .addHttpHeaders("Accept" -> "application/json")
      .addQueryStringParameters("lat" -> lat.toString)
      .addQueryStringParameters("lon" -> lon.toString)
      .addQueryStringParameters("appid" -> config.get[String](API_KEY_PATH))
      .withRequestTimeout(10000.millis)
      .get()
      .map(response => getLatestWeatherFromResponse(response))
  }

  private def getLatestWeatherFromResponse(response: WSResponse): WeatherInfoListDTO = {
    val respStatusCode = response.json \ "cod"
    if (respStatusCode.as[String].toInt == 200) {
      val weatherArray = (response.json \ "list").as[JsArray]
      val weatherInfoList = weatherArray.value.map(
        item =>
          WeatherInfoDTO(
            Option((item \ "dt").as[Long] * 1000),
            (item \ "main" \ "temp").as[Double],
            (item \ "main" \ "temp_min").as[Double],
            (item \ "main" \ "temp_max").as[Double],
            (item \ "weather" \\ "main").map(_.as[String]),
            Option(WindInfoDTO(
              (item \ "wind" \ "speed").asOpt[Double],
              (item \ "wind" \ "deg").asOpt[Double]
            )),
            (item \ "rain").asOpt[String]
          )
      )
      WeatherInfoListDTO(weatherInfoList)
    } else {
      throw new BadRequestToWeatherService("Failed request to weather app")
    }
  }

  /*
    * It doesn't work because I have free account, it requires at least starter subscription
    */
  def getHistoricalWeather(lat: Double, lon: Double): Future[WeatherInfoHistoricalDTO] = {
    val now = LocalDateTime.now()
    val monthBefore = now.minus(1, ChronoUnit.MONTHS)
    println(s"Now: $now, year before: $monthBefore")
    ws.url(HISTORICAL_WEATHER_URL)
      .addHttpHeaders("Accept" -> "application/json")
      .addQueryStringParameters("lat" -> lat.toString)
      .addQueryStringParameters("lon" -> lon.toString)
      .addQueryStringParameters("appid" -> config.get[String](API_KEY_PATH))
      .addQueryStringParameters("start" -> monthBefore.toInstant(ZoneOffset.UTC).toEpochMilli.toString)
      .addQueryStringParameters("end" -> now.toInstant(ZoneOffset.UTC).toEpochMilli.toString)
      .withRequestTimeout(10000.millis)
      .get()
      .map(response => response.body)
    Future(WeatherInfoHistoricalDTO(info, info))
  }
}

object WeatherService {
  val info = WeatherInfoDTO(
    Option.empty,
    0,
    0,
    0,
    Seq(),
    Option(WindInfoDTO(
      Option(0),
      Option(0)
    )),
    Option("")
  )

  val LATEST_DAYS = 3

  val CURRENT_WEATHER_URL = "http://api.openweathermap.org/data/2.5/weather?units=metric"
  val LATEST_WEATHER_URL = "http://api.openweathermap.org/data/2.5/forecast?units=metric"
  val HISTORICAL_WEATHER_URL = "http://history.openweathermap.org/data/2.5/history/city?units=metric"
  val API_KEY_PATH = "weather_api_key"
}