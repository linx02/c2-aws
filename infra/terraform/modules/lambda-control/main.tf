data "aws_caller_identity" "current" {}

resource "aws_iam_role" "role" {
  name = "${var.function_name}-role"
  assume_role_policy = jsonencode({
    Version = "2012-10-17",
    Statement = [{
      Effect = "Allow",
      Principal = { Service = "lambda.amazonaws.com" },
      Action = "sts:AssumeRole"
    }]
  })
  tags = var.tags
}

resource "aws_iam_role_policy" "policy" {
  role = aws_iam_role.role.id
  policy = jsonencode({
    Version = "2012-10-17",
    Statement = [
      {
        Sid      = "Route53ChangeTxt",
        Effect   = "Allow",
        Action   = ["route53:ChangeResourceRecordSets","route53:GetChange","route53:ListResourceRecordSets"],
        Resource = "arn:aws:route53:::hostedzone/${var.zone_id}"
      }
    ]
  })
}

resource "aws_lambda_function" "this" {
  function_name = var.function_name
  role          = aws_iam_role.role.arn
  runtime       = "java21"
  handler       = var.handler
  filename      = var.jar_path
  timeout       = 10
  memory_size   = 512
  environment {
    variables = {
      ZONE_ID     = var.zone_id
      RECORD_NAME = var.record_name
      TTL         = tostring(var.ttl)
    }
  }
  depends_on = [aws_iam_role_policy.policy]
  tags       = var.tags
}