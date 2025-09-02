variable "function_name" {
  type        = string
  default     = "c2-control"
  description = "Lambda function name."
}

variable "zone_id" {
  type        = string
  description = "Hosted zone ID for Route 53 changes."
}

variable "record_name" {
  type        = string
  default     = "_cmd.c2.internal."
  description = "FQDN of the TXT record (e.g. _cmd.c2.internal.)."
}

variable "ttl" {
  type        = number
  default     = 5
  description = "TTL seconds for the TXT record."
}

variable "jar_path" {
  type        = string
  default     = "../../../../lambda-control/target/lambda-control.jar"
  description = "Path to shaded JAR produced by Maven."
}

variable "handler" {
  type        = string
  default     = "org.linx.ControlHandler::handleRequest"
  description = "Fully-qualified handler."
}

variable "tags" {
  type        = map(string)
  default     = {
    Env = "dev"
  }
}