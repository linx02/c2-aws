packer {
  required_plugins {
    amazon = {
      version = ">= 1.0.0"
      source  = "github.com/hashicorp/amazon"
    }
  }
}

variable "aws_region" {
  default = "eu-north-1"
}

variable "subnet_id" {
  type = string
}

variable "ami_name" {
  default = "c2-linux-agent"
}

source "amazon-ebs" "ubuntu" {
  region                 = var.aws_region
  source_ami_filter {
    filters = {
      name                = "ubuntu/images/hvm-ssd/ubuntu-jammy-22.04-amd64-server-*"
      virtualization-type = "hvm"
      root-device-type    = "ebs"
    }
    most_recent = true
    owners      = ["099720109477"] # Canonical
  }
  instance_type          = "t3.micro"
  ssh_username           = "ubuntu"
  associate_public_ip_address = true
  ssh_interface               = "public_ip"
  ssh_timeout                 = "10m"
  ami_name               = var.ami_name
  subnet_id              = var.subnet_id
}

build {
  sources = ["source.amazon-ebs.ubuntu"]

  # Kopiera över .jar fil
  provisioner "file" {
    source      = "../../agent/target/agent.jar"
    destination = "/tmp/agent.jar"
  }

  provisioner "shell" {
    inline_shebang = "/usr/bin/env bash"
    inline = [
      "set -euo pipefail",
      "sudo apt-get update -y",
      "sudo apt-get install -y wget gpg",
      "wget -O- https://apt.corretto.aws/corretto.key | sudo gpg --dearmor -o /usr/share/keyrings/corretto-keyring.gpg",
      "echo 'deb [signed-by=/usr/share/keyrings/corretto-keyring.gpg] https://apt.corretto.aws stable main' | sudo tee /etc/apt/sources.list.d/corretto.list",
      "sudo apt-get update -y",
      "sudo apt-get install -y ruby-full",
      "sudo DEBIAN_FRONTEND=noninteractive apt-get install -y java-21-amazon-corretto-jdk", # Installera Java (Corretto 21 JDK)

      # CodeDeploy
      "cd /tmp",
      "wget https://aws-codedeploy-eu-north-1.s3.eu-north-1.amazonaws.com/latest/install",
      "chmod +x install",
      "sudo ./install auto",
      "sudo systemctl enable codedeploy-agent",

      # XRay Daemon
      "sudo mkdir /opt/xray",
      "cd /opt/xray",
      "sudo wget https://s3.us-east-2.amazonaws.com/aws-xray-assets.us-east-2/xray-daemon/aws-xray-daemon-linux-3.x.zip",
      "sudo unzip aws-xray-daemon-linux-3.x.zip",
      "sudo chown root:root .",
      "sudo chmod 0755 /opt/xray/xray",
      "sudo tee /etc/systemd/system/xray.service > /dev/null <<'EOF'",
      "[Unit]",
      "Description=AWS X-Ray Daemon",
      "After=network.target",
      "",
      "[Service]",
      "Type=simple",
      "ExecStart=/opt/xray/xray --region eu-north-1",
      "Restart=always",
      "User=root",
      "",
      "[Install]",
      "WantedBy=multi-user.target",
      "EOF",
      "sudo systemctl enable xray",

      "sudo mv /tmp/agent.jar /opt/agent.jar",
      "sudo chown root:root /opt/agent.jar",

      # Skapa systemd service
      "sudo tee /etc/systemd/system/c2-agent.service > /dev/null <<EOF",
      "[Unit]",
      "Description=C2 Agent",
      "After=network.target",
      "",
      "[Service]",
      "ExecStart=/usr/bin/java -jar /opt/agent.jar",
      "SuccessExitStatus=143",
      "Restart=always",
      "User=root",
      "",
      "[Install]",
      "WantedBy=multi-user.target",
      "EOF",
      "sudo systemctl enable c2-agent.service"
    ]
  }
}