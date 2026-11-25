data "aws_iam_policy_document" "ec2_assume" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["ec2.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "ec2_ssm" {
  name               = "prj-ec2-ssm"
  assume_role_policy = data.aws_iam_policy_document.ec2_assume.json
}

resource "aws_iam_role_policy_attachment" "ssm_core" {
  role       = aws_iam_role.ec2_ssm.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

resource "aws_iam_instance_profile" "ec2" {
  name = "prj-ec2-ssm"
  role = aws_iam_role.ec2_ssm.name
}

# identité AWS pour GitHub Actions 

# AWS fait confiance aux jetons signés par GitHub
resource "aws_iam_openid_connect_provider" "github" {
  url            = "https://token.actions.githubusercontent.com"
  client_id_list = ["sts.amazonaws.com"]
}

# QUI peut prendre le rôle : seulement le job "production" de MON repo
data "aws_iam_policy_document" "github_assume" {
  statement {
    actions = ["sts:AssumeRoleWithWebIdentity"]
    principals {
      type        = "Federated"
      identifiers = [aws_iam_openid_connect_provider.github.arn]
    }
    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:aud"
      values   = ["sts.amazonaws.com"]
    }
    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:sub"
      values   = ["repo:mohamed22007@149290083/PRJ_deployement@1451786278:environment:production"]
    }
  }
}

resource "aws_iam_role" "github_deploy" {
  name                 = "prj-github-deploy"
  assume_role_policy   = data.aws_iam_policy_document.github_assume.json
  max_session_duration = 3600
}

# CE QU'IL PEUT FAIRE : envoyer une commande à MON instance, et lire le résultat
data "aws_iam_policy_document" "github_deploy" {
  statement {
    sid     = "EnvoyerCommandeDeploiement"
    actions = ["ssm:SendCommand"]
    resources = [
      aws_instance.app.arn,
      "arn:aws:ssm:*::document/AWS-RunShellScript",
    ]
  }
  statement {
    sid       = "LireResultatCommande"
    actions   = ["ssm:GetCommandInvocation"]
    resources = ["*"] # cette action ne supporte pas de restriction par ressource
  }
}

resource "aws_iam_role_policy" "github_deploy" {
  name   = "prj-github-deploy-ssm"
  role   = aws_iam_role.github_deploy.id
  policy = data.aws_iam_policy_document.github_deploy.json
}
