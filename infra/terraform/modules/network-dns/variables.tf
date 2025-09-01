variable "name" {
  type        = string
  default     = "c2"
  description = "Name/prefix for resources."
}

variable "vpc_cidr" {
  type        = string
  default     = "10.80.0.0/16"
  description = "CIDR block for the VPC."
}

variable "zone_name" {
  type        = string
  default     = "c2.internal."
  description = "Private hosted zone name, e.g. c2.internal."
}

variable "create_igw" {
  type        = bool
  default     = false
  description = "Whether to create an Internet Gateway (and make the subnet public)."
}

variable "az" {
  type        = string
  default     = "eu-north-1a"
  description = "Availability Zone for the subnet."
}

variable "tags" {
  type        = map(string)
  default     = {
    Env = "dev"
  }
  description = "Tags applied to resources."
}