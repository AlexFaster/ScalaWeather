
import io.circe.Decoder
import io.circe.generic.semiauto._
import io.circe.parser._
import org.scalajs.dom
import org.scalajs.dom.{Element, Node}
import org.scalajs.dom.ext.Ajax
import org.scalajs.dom.html.Input
import routes.Routes._
import shared.model.{WeatherInfoDTO, WeatherInfoHistoricalDTO, WeatherInfoListDTO, WindInfoDTO}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js.Date
import scala.scalajs.js.timers.setInterval
import scala.util.{Failure, Success}
import scalatags.JsDom.all._

object Main {

  implicit val windDecoder: Decoder[WindInfoDTO] = deriveDecoder[WindInfoDTO]
  implicit val weatherDecoder: Decoder[WeatherInfoDTO] = deriveDecoder[WeatherInfoDTO]
  implicit val weatherListDecoder: Decoder[WeatherInfoListDTO] = deriveDecoder[WeatherInfoListDTO]
  implicit val weatherHistoricalDecoder: Decoder[WeatherInfoHistoricalDTO] = deriveDecoder[WeatherInfoHistoricalDTO]

  private var isCircularRequestExecuted = false
  private val REFRESHING_INTERVAL = 10 * 60 * 1000 //10 minutes

  private val errorBlock = getElementById("error")
  private val currentWeatherBlock = getElementById("current_weather")
  private val followWeatherBlock = getElementById("follow_weather")

  def main(args: Array[String]): Unit = {
    getElementById("button_fetch").addEventListener("click", (event: dom.Event) => {
      fetchWeatherData()
    })
  }

  private def cleanSection(n: Element): Unit = {
    n.innerHTML = ""
  }

  private def fetchWeatherData() = {
    cleanSection(errorBlock)
    cleanSection(currentWeatherBlock)
    cleanSection(followWeatherBlock)
    val lat = getElementById("lat").asInstanceOf[Input].value
    val lon = getElementById("lon").asInstanceOf[Input].value
    var hasErrors = false
    if (!validateLat(lat)) {
      errorBlock.appendChild(getInputErrorUI("lat"))
      hasErrors = true
    }
    if (!validateLon(lon)) {
      errorBlock.appendChild(getInputErrorUI("lon"))
      hasErrors = true
    }

    if (!hasErrors) {
      val weatherServiceFuture = for {
        currentWeather <- Ajax.get(CURRENT_WEATHER_URL + s"?lat=$lat&lon=$lon")
        followWeather <- Ajax.get(FOLLOW_WEATHER_URL + s"?lat=$lat&lon=$lon")
      } yield {
        renderCurrentWeather(currentWeather.responseText)
        renderFollowWeather(followWeather.responseText)
      }

      weatherServiceFuture.recover {
        case e => errorBlock
          .appendChild(getInputErrorUI(s"Somth bad with weather service ${e.getMessage}"))
      }

      if (!isCircularRequestExecuted) {
        setInterval(REFRESHING_INTERVAL) {
          requestCurrentWeather(lat, lon)
          println("requested current weather")
        }
        isCircularRequestExecuted = true
      }
    }
  }

  private def renderCurrentWeather(responseText: String) = {
    val weather = decode[WeatherInfoDTO](responseText).right.get
    cleanSection(currentWeatherBlock)
    currentWeatherBlock.appendChild(getCurrentWeatherUI(weather))
  }

  private def renderFollowWeather(responseText: String) = {
    val weatherList = decode[WeatherInfoListDTO](responseText).right.get
    cleanSection(followWeatherBlock)
    weatherList.weatherInfo.foreach(weatherInfo => {
      followWeatherBlock.appendChild(getCurrentWeatherUI(weatherInfo))
      followWeatherBlock.appendChild(br.render)
    })
  }

  private def requestCurrentWeather(lat: String, lon: String) = {
    Ajax.get(CURRENT_WEATHER_URL + s"?lat=$lat&lon=$lon")
      .onComplete {
        case Success(xhr) =>
          cleanSection(currentWeatherBlock)
          renderCurrentWeather(xhr.responseText)
          println("current weather updated")
        case Failure(e) => errorBlock
          .appendChild(getInputErrorUI(s"Somth bad with weather service ${e.getMessage}"))
      }
  }

  private def getCurrentWeatherUI(weather: WeatherInfoDTO): Node = {
    div(
      weather.date.map(date => {
        s"Current date: ${new Date(date)}"
      }
      ),
      div(span(s"Current temperature: ${weather.currentTemperature}C")),
      div(span(s"Max temperature: ${weather.maxTemperature}C")),
      div(span(s"Min temperature: ${weather.minTemperature}C")),
      div(span(s"Weather State: ${weather.weatherState.mkString(", ")}")),
      weather.windInfo.map(windInfo => {
        div(
          div(span(s"Wind Speed: ${windInfo.speed.getOrElse(0)}")),
          div(span(s"Wind Deg: ${windInfo.deg.getOrElse(0)}"))
        )
      }),
      weather.rainInfo.map(rain => {
        div(span(s"Rain: $rain"))
      })
    ).render
  }

  private def getElementById(id: String) = {
    dom.document.getElementById(id)
  }

  private def validateLon(param: String) = {
    param.matches("-?\\d+") && param.toInt <= 180 && param.toInt >= -180
  }

  private def validateLat(param: String) = {
    param.matches("-?\\d+") && param.toInt <= 90 && param.toInt >= -90
  }

  private def getInputErrorUI(param: String): Node = {
    div(s"Incorrect format $param").render
  }
}
