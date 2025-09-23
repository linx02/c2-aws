locals {
  base = "/c2"
}

resource "aws_ssm_parameter" "record_name" {
  name  = "${local.base}/record_name"
  type  = "String"
  value = var.record_name
}

resource "aws_ssm_parameter" "log_bucket" {
  name  = "${local.base}/log_bucket"
  type  = "String"
  value = var.log_bucket
}

resource "aws_ssm_parameter" "zone_id" {
    name  = "${local.base}/zone_id"
    type  = "String"
    value = var.zone_id
}

resource "aws_ssm_parameter" "log_prefix" {
  name  = "${local.base}/log_prefix"
  type  = "String"
  value = var.log_prefix
}

resource "aws_ssm_parameter" "poll_ms" {
  name  = "${local.base}/poll_ms"
  type  = "String"
  value = tostring(var.poll_ms)
}

resource "aws_ssm_parameter" "control_lambda" {
  name  = "${local.base}/control_lambda"
  type  = "String"
  value = var.control_lambda
}

output "ssm_prefix" {
  description = "Final SSM base path"
  value       = local.base
}