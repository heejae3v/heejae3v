import urllib.parse
import boto3
import uuid
import os
import io
import json
import csv
import sys
import numpy as np
ENDPOINT_NAME = 'T4'
# grab environment variables
ENDPOINT_NAME = os.environ['ENDPOINT_NAME']
runtime= boto3.client('runtime.sagemaker')
s3_client = boto3.client('s3')

def handler(event, context):
 #try:
  for record in event['Records']:
         
   bucket = record['s3']['bucket']['name']
        
   key = record['s3']['object']['key']
   s3_client.download_file(bucket,key,'/tmp/{}'.format(key))
   download_path = '/tmp/{}'.format(key)
   payload = None
   file_path = None
  with open(download_path, 'rb') as f:
    payload = f.read()
    payload = bytearray(payload)
  
    response = runtime.invoke_endpoint(EndpointName=ENDPOINT_NAME,ContentType='application/x-image',
                                   Body=payload)
                                   
    result = json.loads(response['Body'].read().decode())
    index = np.argmax(result)
    image_categories = ['food', 'paper', 'matal', 'glass', 'plastic bag', 'styrofoam','clothing', 'plastic']
    file_name = key [:-4]
    file = file_name + '.txt'
    #print(file)
    file_path = '/tmp/{}'.format(file)
    bucket1 = 't4output'
    sys.stdout = open(file_path, 'w')
    print(image_categories[index] + ", probability - " + str(result[index]))
    sys.stdout.close()
    s3_client.upload_file(file_path, bucket1, file_name + '.txt',
                               ExtraArgs={'ACL': 'public-read', 'ContentType': 'text/plain'})
   #print(sys.stdout)
 #except Exception as e:
                       #print(e)
 
