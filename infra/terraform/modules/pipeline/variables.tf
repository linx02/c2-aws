variable "region" {
  default = "eu-north-1"
}

variable "repo_owner" {
  default = "linx02"
}

variable "repo_name" {
  default = "c2-aws"
}

variable "branch" {
  default = "main"
}

variable "codestar_conn_arn" {
  default = "arn:aws:codeconnections:eu-north-1:796973500025:connection/102117d3-0c11-4e93-92f5-95ef4c4a5b96"
}

variable "cd_app_name" {
  default = "c2-app"
}

variable "cd_deployment_group" {
  default = "c2-dg"
}

variable "cd_ec2_tag_key" {
  default = "Deploy"
}

variable "cd_ec2_tag_value" {
  default = "agent"
}