module "s3_logs" {
  source = "../../modules/s3-logs"
}

module "network_dns" {
  source = "../../modules/network-dns"
}

module "lambda_control" {
  source       = "../../modules/lambda-control"
  zone_id       = module.network_dns.zone_id
  record_name   = "_cmd.${module.network_dns.zone_name}"
}

resource "random_id" "suffix" {
  byte_length = 3
}

output "logs_bucket"  { value = module.s3_logs.bucket_name }
output "zone_id"      { value = module.network_dns.zone_id }
output "zone_name"    { value = module.network_dns.zone_name }
output "vpc_id"       { value = module.network_dns.vpc_id }
output "subnet_ids"   { value = module.network_dns.subnet_id }