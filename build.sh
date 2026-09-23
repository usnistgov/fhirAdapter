set -e

function header_section() {
  echo "\033[1;96m\033[43m\t ** $1 ** \t\x1B[K\033[0m"
}

ROOT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

while getopts ":b:v:lp" flag
do
    case "${flag}" in
        v) VERSION=${OPTARG};;
        l) AS_LATEST=y;;
        p) PUSH_DOCKERHUB=y;;
    esac
done
if [ -z "$VERSION" ]; then
    echo "Image docker version required. (-v)"
    exit 1
fi

if [ -z "$AS_LATEST" ]; then
    AS_LATEST=n
fi

if [ -z "$PUSH_DOCKERHUB" ]; then
    PUSH_DOCKERHUB=n
fi

cd $ROOT_DIR

header_section "Building Docker Image version: $VERSION"
docker buildx build --platform linux/amd64,linux/arm64 -t nist775hit/fits-fhir-adapter:"$VERSION" .

if [ "$AS_LATEST" == "y" ];then
  header_section "Tag version as latest"
  docker tag nist775hit/fits-fhir-adapter:"$VERSION" nist775hit/fits-fhir-adapter:latest
fi

if [ "$PUSH_DOCKERHUB" == "y" ];then
  docker push nist775hit/fits-fhir-adapter:"$VERSION"
  header_section "Image nist775hit/fits-fhir-adapter:$VERSION successfully pushed to DockerHub"
  if [ "$AS_LATEST" == "y" ];then
    docker push nist775hit/fits-fhir-adapter:latest
    header_section "Image nist775hit/fits-fhir-adapter:latest successfully pushed to DockerHub"
  fi
fi