variable "name" {
  type    = string
  default = "c2-linux"
}

variable "vpc_id" {
  type = string
}

variable "public_subnet_id" {
  type = string
}

variable "ami" {
  type = string
}

variable "instance_type" {
  type    = string
  default = "t3.small"
}

variable "my_ip_cidr" {
  type = string
}

variable "key_name" {
  type = string
}

variable "associate_eip" {
  type    = bool
  default = true
}

variable "tags" {
  type    = map(string)
  default = {}
}

variable "region" {
  type        = string
  default     = "eu-north-1"
}

variable "log_bucket" {
  type        = string
}