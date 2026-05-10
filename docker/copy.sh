#!/bin/sh

# 复制项目的文件到对应docker路径，便于一键生成镜像。
usage() {
	echo "Usage: sh copy.sh"
	exit 1
}


# copy sql
echo "begin copy sql "
cp ../sql/ry_20250523.sql ./mysql/db
cp ../sql/ry_config_20260311.sql ./mysql/db

# copy html
echo "begin copy html "
cp -r ../aidevops-ui/dist/** ./nginx/html/dist


# copy jar
echo "begin copy aidevops-gateway "
cp ../aidevops-gateway/target/aidevops-gateway.jar ./aidevops/gateway/jar

echo "begin copy aidevops-auth "
cp ../aidevops-auth/target/aidevops-auth.jar ./aidevops/auth/jar

echo "begin copy aidevops-visual "
cp ../aidevops-modules/aidevops-visual/aidevops-monitor/target/aidevops-visual-monitor.jar  ./aidevops/visual/monitor/jar

echo "begin copy aidevops-modules-system "
cp ../aidevops-modules/aidevops-system/target/aidevops-modules-system.jar ./aidevops/modules/system/jar

echo "begin copy aidevops-modules-file "
cp ../aidevops-modules/aidevops-file/target/aidevops-modules-file.jar ./aidevops/modules/file/jar

echo "begin copy aidevops-modules-job "
cp ../aidevops-modules/aidevops-job/target/aidevops-modules-job.jar ./aidevops/modules/job/jar

echo "begin copy aidevops-modules-gen "
cp ../aidevops-modules/aidevops-gen/target/aidevops-modules-gen.jar ./aidevops/modules/gen/jar

