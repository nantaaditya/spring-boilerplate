FROM bellsoft/liberica-runtime-container:jdk-all-21-musl AS jre-build

# Create a custom Java runtime
RUN jlink \
    --add-modules ALL-MODULE-PATH \
    --strip-debug \
    --no-man-pages \
    --no-header-files \
    --compress=2 \
    --output /javaruntime

RUN strip -p --strip-unneeded javaruntime/lib/server/libjvm.so && \
    find javaruntime -name '*.so' | xargs -i strip -p --strip-unneeded {}

FROM bellsoft/alpaquita-linux-base:latest

ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="${JAVA_HOME}/bin:${PATH}"
COPY --from=jre-build /javaruntime $JAVA_HOME

ENV TZ=Asia/Jakarta
ENV JAVA_OPTS='-server -Xms128m -Xmx512m -XX:+IgnoreUnrecognizedVMOptions -XX:+UseContainerSupport -XX:+IdleTuningCompactOnIdle -XX:+IdleTuningGcOnIdle -Xtune:virtualized -XX:+UseG1GC -XX:MaxHeapFreeRatio=60 -XX:MinHeapFreeRatio=10 -XX:+UseCompressedOops -XX:+UseCompressedClassPointers -XX:InitiatingHeapOccupancyPercent=40 -XX:+OptimizeStringConcat'
LABEL org.opencontainers.image.authors="nantaaditya"

RUN apk update
RUN apk add procps

RUN mkdir app
RUN mkdir app/logs
RUN mkdir app/lib
RUN mkdir app/patch
RUN mkdir app/resources
RUN mkdir app/resources-patch

ADD src/main/resources app/resources
ADD target/*.jar app/lib/
ADD target/dependency/*.jar app/lib/
ADD pom.xml app/pom.xml

WORKDIR /app

ENTRYPOINT /opt/java/openjdk/bin/java $JAVA_OPTS -cp .:resources:patch/*:lib/* com.nantaaditya.example.ExampleApplication