#!/bin/bash

# Function to display usage instructions
usage() {
  echo "Usage: $0 -n <container_name> -i <input_location> -o <output_location>"
  exit 1
}

# Parse command-line arguments
while getopts ":n:i:o:" opt; do
  case $opt in
    n)
      CONTAINER_NAME=$OPTARG
      ;;
    i)
      INPUT_LOCATION=$OPTARG
      ;;
    o)
      OUTPUT_LOCATION=$OPTARG
      ;;
    \?)
      echo "Invalid option: -$OPTARG" >&2
      usage
      ;;
    :)
      echo "Option -$OPTARG requires an argument." >&2
      usage
      ;;
  esac
done

# Check if all required parameters are provided
if [ -z "$CONTAINER_NAME" ] || [ -z "$INPUT_LOCATION" ] || [ -z "$OUTPUT_LOCATION" ]; then
  usage
fi

# Run the Docker container with the provided name and mount the input and output locations
docker run --name "$CONTAINER_NAME" -v "$INPUT_LOCATION":/input -v "$OUTPUT_LOCATION":/output "$CONTAINER_NAME"
