output "vpc_id"          { value = aws_vpc.this.id }
output "subnet_id"       { value = aws_subnet.public.id }
output "route_table_id"  { value = aws_route_table.main.id }
output "zone_id"         { value = aws_route53_zone.private.zone_id }
output "zone_name"       { value = aws_route53_zone.private.name }
output "vpc_cidr"        { value = aws_vpc.this.cidr_block }