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

module "ssm_config" {
  source      = "../../modules/ssm-config"
  log_bucket  = module.s3_logs.bucket_name
  zone_id     = module.network_dns.zone_id
}

module "linux_public" {
  source           = "../../modules/linux-public"
  name             = "c2-linux-demo"
  vpc_id           = module.network_dns.vpc_id
  public_subnet_id = module.network_dns.subnet_id
  ami              = "ami-02ed761ffaf18fcd6"
  key_name         = "c2-demo-key"
  my_ip_cidr       = "213.113.59.153/32"
  associate_eip    = true
  log_bucket  = module.s3_logs.bucket_name
  tags             = { Env = "dev", Project = "c2" }
}

resource "random_id" "suffix" {
  byte_length = 3
}

output "logs_bucket"  { value = module.s3_logs.bucket_name }
output "zone_id"      { value = module.network_dns.zone_id }
output "zone_name"    { value = module.network_dns.zone_name }
output "vpc_id"       { value = module.network_dns.vpc_id }
output "subnet_ids"   { value = module.network_dns.subnet_id }