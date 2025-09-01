# VPC
resource "aws_vpc" "this" {
  cidr_block           = var.vpc_cidr
  enable_dns_support   = true
  enable_dns_hostnames = true
  tags = merge(var.tags, {
    "Name"   = "${var.name}-vpc",
    "Module" = "network-dns"
  })
}

# Internet Gateway (avstängd som default)
resource "aws_internet_gateway" "this" {
  count  = var.create_igw ? 1 : 0
  vpc_id = aws_vpc.this.id
  tags   = merge(var.tags, { "Name" = "${var.name}-igw" })
}

# Subnet
resource "aws_subnet" "main" {
  vpc_id                  = aws_vpc.this.id
  cidr_block              = cidrsubnet(var.vpc_cidr, 4, 0)
  map_public_ip_on_launch = var.create_igw
  availability_zone       = var.az
  tags                    = merge(var.tags, { "Name" = "${var.name}-subnet" })
}

# Route table
resource "aws_route_table" "main" {
  vpc_id = aws_vpc.this.id
  tags   = merge(var.tags, { "Name" = "${var.name}-rt" })
}

resource "aws_route" "default_internet" {
  count                  = var.create_igw ? 1 : 0
  route_table_id         = aws_route_table.main.id
  destination_cidr_block = "0.0.0.0/0"
  gateway_id             = aws_internet_gateway.this[0].id
}

resource "aws_route_table_association" "main" {
  subnet_id      = aws_subnet.main.id
  route_table_id = aws_route_table.main.id
}

# Private Hosted Zone (DNS Zon)
resource "aws_route53_zone" "private" {
  name    = var.zone_name
  comment = "Private hosted zone for C2"

  vpc {
    vpc_id = aws_vpc.this.id
  }

  tags = merge(var.tags, { "Name" = "${var.name}-phz" })
}