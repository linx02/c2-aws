variable "zone_id" {
  type        = string
  description = "Zone ID for Route 53 changes."
}

variable "record_name" {
  type        = string
  default     = "_cmd.c2.internal."
  description = "FQDN for TXT record the agent polls."
}

variable "log_bucket" {
  type        = string
  description = "S3 bucket name for agent logs."
}

variable "log_prefix" {
  type        = string
  default     = "logs/"
  description = "S3 key prefix for logs."
}

variable "control_lambda" {
  type        = string
  default     = "c2-control"
  description = "Control Lambda function name."
}

variable "poll_ms" {
  type        = number
  default     = 5000
  description = "Polling interval in milliseconds."
}