variable "versioning" {
  type        = bool
  default     = true
  description = "Enable S3 bucket versioning."
}

variable "force_destroy" {
  type        = bool
  default     = false
  description = "Allow Terraform to delete non-empty bucket."
}

variable "enable_lifecycle" {
  type        = bool
  default     = true
  description = "Enable lifecycle rule to expire objects."
}

variable "expire_after_days" {
  type        = number
  default     = 30
  description = "Number of days before objects expire."
}

variable "lifecycle_prefix" {
  type        = string
  default     = ""
  description = "Prefix filter for lifecycle rule (e.g., logs/ or artifacts/)."
}

variable "tags" {
  type        = map(string)
  default     = {
    Env = "dev"
  }
  description = "Additional tags."
}