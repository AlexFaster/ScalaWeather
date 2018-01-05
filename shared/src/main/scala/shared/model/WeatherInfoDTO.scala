package shared.model

case class WeatherInfoDTO(
                           date: Option[Long],
                           currentTemperature: Double,
                           minTemperature: Double,
                           maxTemperature: Double,
                           weatherState: Seq[String],
                           windInfo: Option[WindInfoDTO],
                           rainInfo: Option[String]
                         )

