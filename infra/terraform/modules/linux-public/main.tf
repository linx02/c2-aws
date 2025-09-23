locals { tags = merge(var.tags, { Name = var.name }) }

resource "aws_security_group" "sg" {
  name        = "${var.name}-sg"
  description = "SSH from my IP only"
  vpc_id      = var.vpc_id
  tags        = local.tags

  ingress {
    description = "SSH"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = [var.my_ip_cidr]
  }
  egress {
    from_port = 0
    to_port   = 0
    protocol  = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_instance" "this" {
  ami                         = var.ami
  instance_type               = var.instance_type
  subnet_id                   = var.public_subnet_id
  key_name                    = var.key_name
  vpc_security_group_ids      = [aws_security_group.sg.id]
  associate_public_ip_address = true
  tags                        = local.tags
  iam_instance_profile = aws_iam_instance_profile.agent_profile.name
}

resource "aws_eip" "eip" {
  count  = var.associate_eip ? 1 : 0
  domain = "vpc"
  tags   = local.tags
}

resource "aws_eip_association" "assoc" {
  count         = var.associate_eip ? 1 : 0
  instance_id   = aws_instance.this.id
  allocation_id = aws_eip.eip[0].id
}

resource "aws_iam_role" "agent_role" {
  name               = "${var.name}-agent-role"
  assume_role_policy = jsonencode({
    Version = "2012-10-17",
    Statement = [{
      Effect    = "Allow",
      Principal = { Service = "ec2.amazonaws.com" },
      Action    = "sts:AssumeRole"
    }]
  })
  tags = var.tags
}

data "aws_caller_identity" "current" {}

resource "aws_iam_policy" "agent_policy" {
  name = "${var.name}-agent-policy"
  policy = jsonencode({
    Version = "2012-10-17",
    Statement = [
      {
        Effect: "Allow",
        Action: [
          "ssm:GetParameter",
          "ssm:GetParameters",
          "ssm:GetParametersByPath"
        ],
        Resource: [
          "arn:aws:ssm:${var.region}:${data.aws_caller_identity.current.account_id}:parameter/c2",
          "arn:aws:ssm:${var.region}:${data.aws_caller_identity.current.account_id}:parameter/c2/*",
          "arn:aws:ssm:${var.region}:${data.aws_caller_identity.current.account_id}:parameter/c2/dev/agent",
          "arn:aws:ssm:${var.region}:${data.aws_caller_identity.current.account_id}:parameter/c2/dev/agent/*"
        ]
      },
      {
        Effect: "Allow",
        Action: ["s3:PutObject"],
        Resource: "arn:aws:s3:::${var.log_bucket}/*"
      },
      {
        Effect: "Allow",
        Action: ["route53:ListResourceRecordSets","route53:TestDNSAnswer"],
        Resource: "*"
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "agent_attach" {
  role       = aws_iam_role.agent_role.name
  policy_arn = aws_iam_policy.agent_policy.arn
}

resource "aws_iam_instance_profile" "agent_profile" {
  name = "${var.name}-agent-profile"
  role = aws_iam_role.agent_role.name
  tags = var.tags
}

output "instance_id"  { value = aws_instance.this.id }
output "public_ip"    { value = var.associate_eip ? aws_eip.eip[0].public_ip : aws_instance.this.public_ip }
output "public_dns"   { value = aws_instance.this.public_dns }