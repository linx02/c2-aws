import os, json, io, gzip
import boto3
from datetime import datetime

s3 = boto3.client("s3")

def _iter_lines(obj):
    body = obj["Body"].read()
    if obj.get("ContentEncoding") == "gzip" or obj.get("ContentType") == "application/gzip":
        body = gzip.decompress(body)
    return body.decode("utf-8", errors="replace").splitlines()

def lambda_handler(event, _ctx):
    bucket = event["bucket"]
    prefix = event["prefix"]
    date   = event.get("date")
    if not date:
        date = datetime.utcnow().strftime("%Y-%m-%d")

    resp = s3.list_objects_v2(Bucket=bucket, Prefix=prefix)
    contents = resp.get("Contents", [])
    matched  = []
    for o in contents:
        if o["LastModified"].strftime("%Y-%m-%d") == date:
            matched.append(o["Key"])

    total = 0
    successes = 0
    failures = 0
    lines = []

    for key in matched:
        obj = s3.get_object(Bucket=bucket, Key=key)
        file_lines = _iter_lines(obj)
        total += 1
        exit_line = next((l for l in file_lines if l.startswith("# exit:")), None)
        ok = (exit_line and exit_line.strip().endswith("0"))
        if ok: successes += 1
        else: failures += 1

        lines.append(f"- {key}  {'OK' if ok else 'FAIL'}")

    summary = [
        f"Report date: {date}",
        f"Total logs: {total}",
        f"Successes : {successes}",
        f"Failures  : {failures}",
        "",
        *lines
    ]
    return {
        "date": date,
        "reportText": "\n".join(summary),
        "count": total,
        "matchedKeys": matched
    }