package controllers

import javax.inject.{Inject, Singleton}

import exceptions.BadRequestToWeatherService
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}
import service.WeatherService
import shared.model.{WeatherInfoDTO, WeatherInfoHistoricalDTO, WeatherInfoListDTO, WindInfoDTO}

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class WeatherController @Inject()(
                                   cc: ControllerComponents,
                                   weatherService: WeatherService
                                 ) extends AbstractController(cc) {

  implicit val windInfoFormat = Json.format[WindInfoDTO]
  implicit val weatherFormat = Json.format[WeatherInfoDTO]
  implicit val weatherHistoricalFormat = Json.format[WeatherInfoHistoricalDTO]
  implicit val weatherListFormat = Json.format[WeatherInfoListDTO]

  def getCurrentWeather(lat: Double, lon: Double) = Action.async {
    weatherService.getCurrentWeather(lat, lon)
      .map(
        apiResponse => Ok(Json.toJson(apiResponse))
      )
      .recover {
        case e: BadRequestToWeatherService => {
          BadRequest(e.getMessage)
        }
        case e: Throwable => {
          InternalServerError(e.getMessage)
        }
      }
  }

  def getLatestWeather(lat: Double, lon: Double) = Action.async {
    weatherService.getLatestWeather(lat, lon)
      .map(
        apiResponse => Ok(Json.toJson(apiResponse))
      )
      .recover {
        case e: BadRequestToWeatherService => {
          BadRequest(e.getMessage)
        }
        case e: Throwable => {
          InternalServerError(e.getMessage)
        }
      }


  }

  def getHistoricalWeather(lat: Double, lon: Double) = Action.async {
    weatherService.getHistoricalWeather(lat, lon)
      .map(
        apiResponse => Ok(Json.toJson(apiResponse))
      )
      .recover {
        case e: BadRequestToWeatherService => {
          BadRequest(e.getMessage)
        }
        case e: Throwable => {
          InternalServerError(e.getMessage)
        }
      }
  }

}
