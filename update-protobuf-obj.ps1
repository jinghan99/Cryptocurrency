# 假设本地环境已经配置好protoc
$SRC_DIR="D:\workspace\northstar\northstar-api\src\main\resources\proto"
$JAVA_DIR="D:\workspace\northstar\northstar-api\src\main\java"
$JS_DIR="D:\workspace\northstar\northstar-monitor\src\lib"
protoc --proto_path=$SRC_DIR --java_out=$JAVA_DIR --js_out=import_style=commonjs,binary:$JS_DIR $SRC_DIR/xyz/redtorch/pb/core_field.proto
protoc --proto_path=$SRC_DIR --java_out=$JAVA_DIR --js_out=import_style=commonjs,binary:$JS_DIR $SRC_DIR/xyz/redtorch/pb/core_enum.proto