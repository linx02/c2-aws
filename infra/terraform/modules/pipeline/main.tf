data "aws_caller_identity" "me" {}

resource "aws_s3_bucket" "artifacts" {
  bucket        = "c2-pipeline-artifacts-${data.aws_caller_identity.me.account_id}"
  force_destroy = true
}

resource "aws_s3_bucket_policy" "artifacts_allow_ec2_read" {
  bucket = aws_s3_bucket.artifacts.id
  policy = jsonencode({
    Version = "2012-10-17",
    Statement = [
      {
        Sid       = "AllowEC2InstanceRoleReadArtifacts"
        Effect    = "Allow"
        Principal = {
          AWS = "arn:aws:iam::${data.aws_caller_identity.me.account_id}:role/${var.agent_instance_role_name}"
        }
        Action   = ["s3:GetObject", "s3:GetObjectVersion"]
        Resource = "${aws_s3_bucket.artifacts.arn}/*"
      },
      {
        Sid       = "AllowEC2InstanceRoleListArtifacts"
        Effect    = "Allow"
        Principal = {
          AWS = "arn:aws:iam::${data.aws_caller_identity.me.account_id}:role/${var.agent_instance_role_name}"
        }
        Action   = ["s3:ListBucket"]
        Resource = aws_s3_bucket.artifacts.arn
      }
    ]
  })
}

resource "aws_iam_role" "codepipeline_role" {
  name               = "c2-pipeline-role"
  assume_role_policy = jsonencode({
    Version = "2012-10-17",
    Statement = [{
      Effect    = "Allow",
      Principal = { Service = "codepipeline.amazonaws.com" },
      Action    = "sts:AssumeRole"
    }]
  })
}

resource "aws_iam_role_policy" "codepipeline_policy" {
  role = aws_iam_role.codepipeline_role.id
  policy = jsonencode({
    Version = "2012-10-17",
    Statement = [
      {
        Effect   = "Allow",
        Action   = ["s3:GetObject","s3:GetObjectVersion","s3:PutObject","s3:PutObjectAcl","s3:ListBucket"],
        Resource = [
          aws_s3_bucket.artifacts.arn,
          "${aws_s3_bucket.artifacts.arn}/*"
        ]
      },
      {
        Effect   = "Allow",
        Action   = ["codebuild:BatchGetBuilds", "codebuild:StartBuild"],
        Resource = "*"
      },
      {
        Effect   = "Allow",
        Action   = ["codestar-connections:UseConnection"],
        Resource = var.codestar_conn_arn
      },
      {
        Effect = "Allow",
        Action = [
          "codedeploy:CreateDeployment",
          "codedeploy:GetDeployment",
          "codedeploy:RegisterApplicationRevision",
          "codedeploy:GetApplicationRevision",
          "codedeploy:GetDeploymentConfig",
          "codedeploy:GetApplication",
          "codedeploy:ListDeploymentGroups",
          "codedeploy:ListApplications"
        ],
        Resource = "*"
      }
    ]
  })
}

resource "aws_iam_role" "codebuild_role" {
  name               = "c2-codebuild-role"
  assume_role_policy = jsonencode({
    Version = "2012-10-17",
    Statement = [{
      Effect    = "Allow",
      Principal = { Service = "codebuild.amazonaws.com" },
      Action    = "sts:AssumeRole"
    }]
  })
}

resource "aws_iam_role_policy" "codebuild_policy" {
  role = aws_iam_role.codebuild_role.id
  policy = jsonencode({
    Version = "2012-10-17",
    Statement = [
      {
        Effect = "Allow",
        Action = [
          "logs:CreateLogGroup",
          "logs:CreateLogStream",
          "logs:PutLogEvents",
          "logs:DescribeLogStreams"
        ],
        Resource = "*"
      },

      {
        Effect = "Allow",
        Action = [
          "s3:GetObject",
          "s3:GetObjectVersion",
          "s3:PutObject"
        ],
        Resource = "${aws_s3_bucket.artifacts.arn}/*"
      },
      {
        Effect = "Allow",
        Action = [
          "s3:ListBucket"
        ],
        Resource = aws_s3_bucket.artifacts.arn
      }
    ]
  })
}

resource "aws_codebuild_project" "tests" {
  name         = "c2-tests"
  service_role = aws_iam_role.codebuild_role.arn

  artifacts { type = "CODEPIPELINE" }

  environment {
    compute_type = "BUILD_GENERAL1_SMALL"
    image        = "aws/codebuild/standard:7.0"
    type         = "LINUX_CONTAINER"
    environment_variable {
      name  = "AWS_REGION"
      value = var.region
    }
  }

  source {
    type     = "CODEPIPELINE"
    buildspec = "buildspec.yml"
  }

  cache { type = "NO_CACHE" }
}

resource "aws_iam_role" "codedeploy_role" {
  name = "c2-codedeploy-role"
  assume_role_policy = jsonencode({
    Version = "2012-10-17",
    Statement = [{
      Effect    = "Allow",
      Principal = { Service = "codedeploy.amazonaws.com" },
      Action    = "sts:AssumeRole"
    }]
  })
}

resource "aws_iam_role_policy" "codedeploy_policy" {
  role = aws_iam_role.codedeploy_role.id
  policy = jsonencode({
    Version = "2012-10-17",
    Statement = [
      {
        Effect = "Allow",
        Action = [
          "ec2:DescribeInstances",
          "ec2:DescribeInstanceStatus",
          "ec2:DescribeTags",
          "ec2:DescribeRegions"
        ],
        Resource = "*"
      },

      {
        Effect = "Allow",
        Action = ["s3:GetObject", "s3:GetObjectVersion"],
        Resource = "${aws_s3_bucket.artifacts.arn}/*"
      },
      {
        Effect = "Allow",
        Action = ["s3:ListBucket"],
        Resource = aws_s3_bucket.artifacts.arn
      }
    ]
  })
}

resource "aws_codedeploy_app" "app" {
  name             = var.cd_app_name
  compute_platform = "Server"
}

resource "aws_codedeploy_deployment_group" "dg" {
  app_name              = aws_codedeploy_app.app.name
  deployment_group_name = var.cd_deployment_group
  service_role_arn      = aws_iam_role.codedeploy_role.arn
  deployment_config_name = "CodeDeployDefault.AllAtOnce"

  ec2_tag_set {
    ec2_tag_filter {
      key   = var.cd_ec2_tag_key
      type  = "KEY_AND_VALUE"
      value = var.cd_ec2_tag_value
    }
  }

  auto_rollback_configuration {
    enabled = false
  }
}

resource "aws_codepipeline" "c2" {
  name     = "c2-pipeline"
  role_arn = aws_iam_role.codepipeline_role.arn

  artifact_store {
    type     = "S3"
    location = aws_s3_bucket.artifacts.bucket
  }

  stage {
    name = "Source"
    action {
      name             = "GitHub"
      category         = "Source"
      owner            = "AWS"
      provider         = "CodeStarSourceConnection"
      version          = "1"
      output_artifacts = ["source"]
      configuration = {
        ConnectionArn   = var.codestar_conn_arn
        FullRepositoryId = "${var.repo_owner}/${var.repo_name}"
        BranchName      = var.branch
        DetectChanges   = "true"
      }
    }
  }

  stage {
    name = "BuildAndTest"
    action {
      name             = "MavenTestsAndBundle"
      category         = "Build"
      owner            = "AWS"
      provider         = "CodeBuild"
      input_artifacts  = ["source"]
      output_artifacts = ["bundle"]
      version          = "1"
      configuration = {
        ProjectName = aws_codebuild_project.tests.name
      }
    }
  }

  stage {
    name = "Deploy"
    action {
      name             = "CodeDeployToEC2"
      category         = "Deploy"
      owner            = "AWS"
      provider         = "CodeDeploy"
      input_artifacts  = ["bundle"]
      version          = "1"
      configuration = {
        ApplicationName     = aws_codedeploy_app.app.name
        DeploymentGroupName = aws_codedeploy_deployment_group.dg.deployment_group_name
      }
    }
  }
}

variable "agent_instance_role_name" {
  description = "Name of the EC2 instance IAM role that runs the CodeDeploy agent (used to grant S3 read on artifacts bucket)."
  type        = string
  default     = "c2-linux-demo-agent-role"
}