resource "random_id" "suffix" {
  byte_length = 3
}

locals {
  bucket_name = (
    "c2-logs-${random_id.suffix.hex}"
  )
}

# Bucket
resource "aws_s3_bucket" "logs" {
  bucket        = local.bucket_name
  force_destroy = var.force_destroy
  tags          = merge(var.tags, { "Module" = "s3-logs" })
}

# Versioning
resource "aws_s3_bucket_versioning" "this" {
  bucket = aws_s3_bucket.logs.id
  versioning_configuration { status = var.versioning ? "Enabled" : "Suspended" }
}

# Kryptering
resource "aws_s3_bucket_server_side_encryption_configuration" "this" {
  bucket = aws_s3_bucket.logs.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

# Blockera publik åtkomst
resource "aws_s3_bucket_public_access_block" "this" {
  bucket                  = aws_s3_bucket.logs.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# Ta bort gamla loggar
resource "aws_s3_bucket_lifecycle_configuration" "this" {
  bucket = aws_s3_bucket.logs.id
  rule {
    id     = "expire-old-logs"
    status = var.enable_lifecycle ? "Enabled" : "Disabled"
    expiration { days = var.expire_after_days }
    filter { prefix = var.lifecycle_prefix }
  }
}