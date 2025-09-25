import os, json, boto3

s3 = boto3.client("s3")

def lambda_handler(event, _ctx):
    bucket     = event["bucket"]
    report_key = f"reports/report-{event['date']}.txt"
    body       = event["reportText"]
    s3.put_object(Bucket=bucket, Key=report_key, Body=body.encode("utf-8"))
    return {"reportKey": report_key}