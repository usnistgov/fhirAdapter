set -e

function header_section() {
  echo "\033[1;96m\033[43m\t ** $1 ** \t\x1B[K\033[0m"
}

ROOT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
TMP_LOCAL_REPO="$(mktemp -d)"

header_section "Temporary Local Maven Repo Created @ $TMP_LOCAL_REPO"

while getopts ":b:v:f:lp" flag
do
    case "${flag}" in
        f) FHIR_LIB=${OPTARG};;
        v) VERSION=${OPTARG};;
        l) AS_LATEST=y;;
        p) PUSH_DOCKERHUB=y;;
    esac
done

if [ -z "$FHIR_LIB" ]; then
    echo "FHIR Lib project path is required. (-b)"
    exit 1
fi

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

header_section "Building FHIR Lib"
cd $FHIR_LIB
mvn clean install -Dmaven.repo.local=$TMP_LOCAL_REPO

header_section "Building FITS FHIR Adapter"
cd $ROOT_DIR
mvn clean install -Dmaven.repo.local=$TMP_LOCAL_REPO

header_section "Deleting Temporary Local Maven Repo @ $TMP_LOCAL_REPO"
rm -rf $TMP_LOCAL_REPO

cd $ROOT_DIR

header_section "Building Docker Image version: $VERSION"
docker buildx build --platform linux/amd64,linux/arm64 -t nist775hit/fits-fhir-adapter:$VERSION .

if [ "$AS_LATEST" == "y" ];then
  header_section "Tag version as latest"
  docker tag nist775hit/fits-fhir-adapter:$VERSION nist775hit/fits-fhir-adapter:latest
fi

if [ "$PUSH_DOCKERHUB" == "y" ];then
  docker push nist775hit/fits-fhir-adapter:$VERSION
  header_section "Image nist775hit/fits-fhir-adapter:$VERSION successfully pushed to DockerHub"
fi