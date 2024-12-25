# pip install paramiko python_dotenv

import os
import paramiko
from dotenv import load_dotenv

# Load environment variables from .env file
load_dotenv()

# Retrieve environment variables
host = os.getenv("HOST")
username = os.getenv("SFTP_USERNAME")
password = os.getenv("SFTP_PASSWORD")
port = int(os.getenv("PORT", 22))  # Default to port 22 if not specified
local_file_path = os.getenv("LOCAL_FILE_PATH")
remote_file_path = os.getenv("REMOTE_FILE_PATH")

# Validate variables
if not all([host, username, password, port, local_file_path, remote_file_path]):
    raise ValueError("One or more environment variables are missing or invalid.")

# Connect to the SFTP server and upload the file
try:
    print(f"Connecting to {host}:{port}...")
    transport = paramiko.Transport((host, port))
    transport.connect(username=username, password=password)

    sftp = paramiko.SFTPClient.from_transport(transport)
    print(f"Uploading {local_file_path} to {remote_file_path}...")
    sftp.put(local_file_path, remote_file_path)
    print("File uploaded successfully.")

    sftp.close()
    transport.close()

except Exception as e:
    print(f"An error occurred: {e}")
