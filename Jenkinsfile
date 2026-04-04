pipeline {
  agent {
    kubernetes {
      defaultContainer 'builder'
      yaml """
apiVersion: v1
kind: Pod
metadata:
  labels:
    app: aidevops-test-pipeline
spec:
  serviceAccountName: jenkins-agent
  restartPolicy: Never
  containers:
    - name: builder
      image: harbor.zoudekang.cloud/aidevops/jenkins-agent-tools:20260401
      imagePullPolicy: Always
      command: [\"cat\"]
      tty: true
      volumeMounts:
        - name: m2-cache
          mountPath: /home/ubuntu/.m2
        - name: npm-cache
          mountPath: /home/ubuntu/.npm
    - name: helm
      image: harbor.zoudekang.cloud/ci-tools/helm:3.16.4
      imagePullPolicy: IfNotPresent
      command: [\"cat\"]
      tty: true
  volumes:
    - name: m2-cache
      persistentVolumeClaim:
        claimName: jenkins-m2-cache-pvc
    - name: npm-cache
      persistentVolumeClaim:
        claimName: jenkins-npm-cache-pvc
"""
    }
  }

  options {
    timestamps()
    disableConcurrentBuilds()
    buildDiscarder(logRotator(numToKeepStr: '20'))
  }

  environment {
    REGISTRY = 'harbor.zoudekang.cloud'
    TEST_PROJECT = 'aidevops-test'
    K8S_NAMESPACE = 'aidevops-test'
    BUILD_NAMESPACE = 'jenkins'
    GIT_REPO = 'https://github.com/dekangzou/DevOps.git'
    GIT_BRANCH = 'test'
    CI_TOOLS_IMAGE = 'harbor.zoudekang.cloud/aidevops/jenkins-agent-tools:20260401'
    KANIKO_IMAGE = 'harbor.zoudekang.cloud/ci-tools/kaniko-executor:v1.23.2-debug'
  }

  stages {
    stage('Acquire Shared Release Lock') {
      steps {
        container('builder') {
          sh '''
            set -eu
            LOCK_NAME=aidevops-shared-release-lock
            LOCK_HOLDER="${JOB_NAME}-${BUILD_NUMBER}"
            echo "trying to acquire $LOCK_NAME as $LOCK_HOLDER"
            while true; do
              if kubectl -n ${BUILD_NAMESPACE} create configmap "$LOCK_NAME" \
                --from-literal=holder="$LOCK_HOLDER" \
                --from-literal=job="${JOB_NAME}" \
                --from-literal=build="${BUILD_NUMBER}" >/dev/null 2>&1; then
                echo "acquired shared release lock: $LOCK_NAME"
                break
              fi
              CURRENT=$(kubectl -n ${BUILD_NAMESPACE} get configmap "$LOCK_NAME" -o jsonpath='{.data.holder}' 2>/dev/null || true)
              echo "lock busy, current holder=${CURRENT:-unknown}, waiting..."
              sleep 10
            done
          '''
        }
      }
    }

    stage('Checkout') {
      steps {
        checkout scm
        script {
          sh '''
            git config --global --add safe.directory "$WORKSPACE"
            git config --global --add safe.directory "$(pwd)"
          '''
          env.GIT_COMMIT_SHORT = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
          env.GIT_COMMIT_MSG_RAW = sh(script: 'git log -1 --pretty=%s', returnStdout: true).trim()
          env.GIT_COMMIT_TAG = sh(
            script: '''
              MSG=$(git log -1 --pretty=%s | tr '[:upper:]' '[:lower:]')
              MSG=$(printf "%s" "$MSG" | tr -cs 'a-z0-9._-' '-' | sed 's/^-*//; s/-*$//')
              if [ -z "$MSG" ]; then MSG=commit; fi
              printf "%s-%s" "$MSG" "$(git rev-parse --short HEAD)"
            ''',
            returnStdout: true
          ).trim()
          currentBuild.displayName = "#${env.BUILD_NUMBER} ${env.GIT_COMMIT_TAG}"
        }
      }
    }

    stage('Detect Changed Services') {
      steps {
        script {
          def changed = sh(
            script: '''
              if git rev-parse HEAD~1 >/dev/null 2>&1; then
                git diff --name-only HEAD~1 HEAD
              else
                git ls-files
              fi
            ''',
            returnStdout: true
          ).trim().split('\n') as List

          def targets = [] as Set
          changed.each { f ->
            if (!f?.trim()) return
            if (f.startsWith('aidevops-auth/')) targets << 'auth'
            if (f.startsWith('aidevops-gateway/')) targets << 'gateway'
            if (f.startsWith('aidevops-modules/aidevops-system/')) targets << 'system'
            if (f.startsWith('aidevops-ui/')) targets << 'ui'
            if (f == 'pom.xml' || f.startsWith('aidevops-common/') || f.startsWith('aidevops-api/') || f.startsWith('aidevops-modules/pom.xml') || f.startsWith('aidevops-common/pom.xml')) {
              targets << 'auth'; targets << 'gateway'; targets << 'system'
            }
            if (f.startsWith('docker/build/') || f == 'Jenkinsfile' || f == '.dockerignore' || f.startsWith('deploy/')) {
              targets << 'auth'; targets << 'gateway'; targets << 'system'; targets << 'ui'
            }
          }

          env.AI_CHAT_CONFIG_CHANGED = changed.any { f ->
            f?.startsWith('deploy/helm/aidevops-cloud/') ||
            f == 'Jenkinsfile' ||
            f?.startsWith('aidevops-modules/aidevops-system/src/main/java/com/aidevops/system/controller/AiChatController.java') ||
            f?.startsWith('aidevops-modules/aidevops-system/src/main/java/com/aidevops/system/config/AiChatProperties.java') ||
            f?.startsWith('aidevops-modules/aidevops-system/src/main/java/com/aidevops/system/service/') ||
            f?.startsWith('aidevops-ui/src/views/ai/chat/') ||
            f?.startsWith('aidevops-ui/src/api/ai/chat.js')
          } ? 'true' : 'false'

          env.BUILD_AUTH = targets.contains('auth') ? 'true' : 'false'
          env.BUILD_GATEWAY = targets.contains('gateway') ? 'true' : 'false'
          env.BUILD_SYSTEM = targets.contains('system') ? 'true' : 'false'
          env.BUILD_UI = targets.contains('ui') ? 'true' : 'false'
          env.SKIP_PIPELINE = targets.isEmpty() ? 'true' : 'false'

          if (env.SKIP_PIPELINE == 'true') {
            currentBuild.description = 'No relevant changes detected'
          }
          echo "Changed services => auth=${env.BUILD_AUTH}, gateway=${env.BUILD_GATEWAY}, system=${env.BUILD_SYSTEM}, ui=${env.BUILD_UI}, aiChatConfig=${env.AI_CHAT_CONFIG_CHANGED}"
        }
      }
    }


    stage('SonarQube Code Analysis') {
      when {
        expression { env.BRANCH_NAME == 'test' && env.SKIP_PIPELINE != 'true' && (env.BUILD_AUTH == 'true' || env.BUILD_GATEWAY == 'true' || env.BUILD_SYSTEM == 'true') }
      }
      steps {
        container('builder') {
          withCredentials([usernamePassword(credentialsId: 'sonarqube-token', usernameVariable: 'SQ_USER', passwordVariable: 'SQ_PASS')]) {
            sh '''
              set -eu
              SQ_HOST="http://sonarqube.sonarqube:9000/sonarqube"

              git clone --branch ${GIT_BRANCH} --single-branch ${GIT_REPO} /tmp/sonar-src

              if [ "${BUILD_AUTH}" = "true" ]; then
                echo "[sonar] analyzing aidevops-auth ..."
                mvn verify sonar:sonar \
                  -f /tmp/sonar-src/pom.xml \
                  -pl aidevops-auth -am \
                  -Dsonar.token=${SQ_PASS} \
                  -Dsonar.host.url=${SQ_HOST} \
                  -Dsonar.projectKey=auth \
                  -Dsonar.projectName=auth \
                  -Dsonar.skipTests=true \
                  -Dsonar.inclusions="**/*.java"
                echo "[sonar] aidevops-auth done"
              fi

              if [ "${BUILD_GATEWAY}" = "true" ]; then
                echo "[sonar] analyzing aidevops-gateway ..."
                mvn verify sonar:sonar \
                  -f /tmp/sonar-src/pom.xml \
                  -pl aidevops-gateway -am \
                  -Dsonar.token=${SQ_PASS} \
                  -Dsonar.host.url=${SQ_HOST} \
                  -Dsonar.projectKey=gateway \
                  -Dsonar.projectName=gateway \
                  -Dsonar.skipTests=true \
                  -Dsonar.inclusions="**/*.java"
                echo "[sonar] aidevops-gateway done"
              fi

              if [ "${BUILD_SYSTEM}" = "true" ]; then
                echo "[sonar] analyzing aidevops-modules/aidevops-system ..."
                mvn verify sonar:sonar \
                  -f /tmp/sonar-src/pom.xml \
                  -pl aidevops-modules/aidevops-system -am \
                  -Dsonar.token=${SQ_PASS} \
                  -Dsonar.host.url=${SQ_HOST} \
                  -Dsonar.projectKey=system \
                  -Dsonar.projectName=system \
                  -Dsonar.skipTests=true \
                  -Dsonar.inclusions="**/*.java"
                echo "[sonar] aidevops-modules/aidevops-system done"
              fi

              rm -rf /tmp/sonar-src
              echo "[sonar] all analyses complete"
            '''
          }
        }
      }
    }

    stage('Build And Push In Dedicated Pods') {
      when {
        expression { env.SKIP_PIPELINE != 'true' }
      }
      steps {
        container('builder') {
          sh '''
            set -eu

            wait_pod() {
              local pod_name="$1"
              local timeout_secs="${2:-1800}"
              local elapsed=0
              while true; do
                phase=$(kubectl -n ${BUILD_NAMESPACE} get pod "$pod_name" -o jsonpath='{.status.phase}' 2>/dev/null || true)
                case "$phase" in
                  Succeeded)
                    echo "[ok] $pod_name succeeded"
                    break
                    ;;
                  Failed)
                    echo "[fail] $pod_name failed"
                    kubectl -n ${BUILD_NAMESPACE} describe pod "$pod_name" || true
                    kubectl -n ${BUILD_NAMESPACE} logs "$pod_name" --all-containers=true || true
                    return 1
                    ;;
                  '')
                    echo "[wait] $pod_name not created yet"
                    ;;
                  *)
                    echo "[wait] $pod_name phase=$phase"
                    ;;
                esac
                if [ "$elapsed" -ge "$timeout_secs" ]; then
                  echo "[timeout] $pod_name exceeded ${timeout_secs}s"
                  kubectl -n ${BUILD_NAMESPACE} describe pod "$pod_name" || true
                  kubectl -n ${BUILD_NAMESPACE} logs "$pod_name" --all-containers=true || true
                  return 1
                fi
                sleep 5
                elapsed=$((elapsed + 5))
              done
            }

            cleanup_pod() {
              kubectl -n ${BUILD_NAMESPACE} delete pod "$1" --ignore-not-found=true >/dev/null 2>&1 || true
            }

            apply_backend_pod() {
              local pod_name="$1"
              local module="$2"
              local jar_path="$3"
              local image_name="$4"
              cat <<YAML | kubectl apply -f -
apiVersion: v1
kind: Pod
metadata:
  name: ${pod_name}
  namespace: ${BUILD_NAMESPACE}
  labels:
    app: ci-build
    ci-role: backend-builder
    ci-service: ${image_name}
spec:
  restartPolicy: Never
  serviceAccountName: jenkins-agent
  imagePullSecrets:
    - name: harbor-regcred
  affinity:
    podAntiAffinity:
      preferredDuringSchedulingIgnoredDuringExecution:
        - weight: 100
          podAffinityTerm:
            topologyKey: kubernetes.io/hostname
            labelSelector:
              matchLabels:
                app: ci-build
  topologySpreadConstraints:
    - maxSkew: 1
      topologyKey: kubernetes.io/hostname
      whenUnsatisfiable: ScheduleAnyway
      labelSelector:
        matchLabels:
          app: ci-build
  volumes:
    - name: workspace
      emptyDir: {}
    - name: m2-cache
      persistentVolumeClaim:
        claimName: jenkins-m2-cache-pvc
    - name: kaniko-cache
      persistentVolumeClaim:
        claimName: jenkins-kaniko-cache-pvc
    - name: harbor-config
      secret:
        secretName: harbor-regcred
        items:
          - key: .dockerconfigjson
            path: config.json
  initContainers:
    - name: builder
      image: ${CI_TOOLS_IMAGE}
      imagePullPolicy: Always
      command: ["sh","-lc"]
      args:
        - |
          set -e
          git clone --branch ${GIT_BRANCH} --single-branch ${GIT_REPO} /workspace/src
          cd /workspace/src
          mvn -T 1C -DskipTests package -pl ${module} -am
      volumeMounts:
        - name: workspace
          mountPath: /workspace
        - name: m2-cache
          mountPath: /home/ubuntu/.m2
  containers:
    - name: kaniko
      image: ${KANIKO_IMAGE}
      imagePullPolicy: IfNotPresent
      args:
        - --context=/workspace/src
        - --dockerfile=/workspace/src/docker/build/backend.Dockerfile
        - --destination=${REGISTRY}/${TEST_PROJECT}/${image_name}:${GIT_COMMIT_TAG}
        - --snapshot-mode=redo
        - --use-new-run
        - --cache=true
        - --cache-dir=/kaniko-cache
        - --cache-ttl=336h
        - --skip-tls-verify-registry=${REGISTRY}
        - --build-arg=JAR_PATH=${jar_path}
      volumeMounts:
        - name: workspace
          mountPath: /workspace
        - name: kaniko-cache
          mountPath: /kaniko-cache
        - name: harbor-config
          mountPath: /kaniko/.docker
YAML
            }

            apply_ui_pod() {
              local pod_name="$1"
              cat <<YAML | kubectl apply -f -
apiVersion: v1
kind: Pod
metadata:
  name: ${pod_name}
  namespace: ${BUILD_NAMESPACE}
  labels:
    app: ci-build
    ci-role: ui-builder
    ci-service: aidevops-ui
spec:
  restartPolicy: Never
  serviceAccountName: jenkins-agent
  imagePullSecrets:
    - name: harbor-regcred
  affinity:
    podAntiAffinity:
      preferredDuringSchedulingIgnoredDuringExecution:
        - weight: 100
          podAffinityTerm:
            topologyKey: kubernetes.io/hostname
            labelSelector:
              matchLabels:
                app: ci-build
  topologySpreadConstraints:
    - maxSkew: 1
      topologyKey: kubernetes.io/hostname
      whenUnsatisfiable: ScheduleAnyway
      labelSelector:
        matchLabels:
          app: ci-build
  volumes:
    - name: workspace
      emptyDir: {}
    - name: npm-cache
      persistentVolumeClaim:
        claimName: jenkins-npm-cache-pvc
    - name: kaniko-cache
      persistentVolumeClaim:
        claimName: jenkins-kaniko-cache-pvc
    - name: harbor-config
      secret:
        secretName: harbor-regcred
        items:
          - key: .dockerconfigjson
            path: config.json
  initContainers:
    - name: builder
      image: ${CI_TOOLS_IMAGE}
      imagePullPolicy: Always
      command: ["sh","-lc"]
      args:
        - |
          set -e
          git clone --branch ${GIT_BRANCH} --single-branch ${GIT_REPO} /workspace/src
          cd /workspace/src/aidevops-ui
          npm config set registry https://registry.npmmirror.com --location=user
          if [ -f package-lock.json ]; then
            npm ci --legacy-peer-deps
          else
            npm install --legacy-peer-deps
          fi
          npm run build:prod
          test -d dist
      volumeMounts:
        - name: workspace
          mountPath: /workspace
        - name: npm-cache
          mountPath: /home/ubuntu/.npm
        - name: kaniko-cache
          mountPath: /kaniko-cache
  containers:
    - name: kaniko
      image: ${KANIKO_IMAGE}
      imagePullPolicy: IfNotPresent
      args:
        - --context=/workspace/src
        - --dockerfile=/workspace/src/docker/build/frontend.Dockerfile
        - --destination=${REGISTRY}/${TEST_PROJECT}/aidevops-ui:${GIT_COMMIT_TAG}
        - --snapshot-mode=redo
        - --use-new-run
        - --cache=true
        - --cache-dir=/kaniko-cache
        - --cache-ttl=336h
        - --skip-tls-verify-registry=${REGISTRY}
      volumeMounts:
        - name: workspace
          mountPath: /workspace
        - name: harbor-config
          mountPath: /kaniko/.docker
YAML
            }

            AUTH_POD="build-auth-${BUILD_NUMBER}"
            GATEWAY_POD="build-gateway-${BUILD_NUMBER}"
            SYSTEM_POD="build-system-${BUILD_NUMBER}"
            UI_POD="build-ui-${BUILD_NUMBER}"

            cleanup_pod "$AUTH_POD"
            cleanup_pod "$GATEWAY_POD"
            cleanup_pod "$SYSTEM_POD"
            cleanup_pod "$UI_POD"

            pids=""
            if [ "${BUILD_AUTH}" = "true" ]; then apply_backend_pod "$AUTH_POD" 'aidevops-auth' 'aidevops-auth/target/aidevops-auth.jar' 'aidevops-auth' & pids="$pids $!"; fi
            if [ "${BUILD_GATEWAY}" = "true" ]; then apply_backend_pod "$GATEWAY_POD" 'aidevops-gateway' 'aidevops-gateway/target/aidevops-gateway.jar' 'aidevops-gateway' & pids="$pids $!"; fi
            if [ "${BUILD_SYSTEM}" = "true" ]; then apply_backend_pod "$SYSTEM_POD" 'aidevops-modules/aidevops-system' 'aidevops-modules/aidevops-system/target/aidevops-modules-system.jar' 'aidevops-system' & pids="$pids $!"; fi
            if [ "${BUILD_UI}" = "true" ]; then apply_ui_pod "$UI_POD" & pids="$pids $!"; fi
            for pid in $pids; do wait "$pid"; done

            pids=""
            if [ "${BUILD_AUTH}" = "true" ]; then wait_pod "$AUTH_POD" & pids="$pids $!"; fi
            if [ "${BUILD_GATEWAY}" = "true" ]; then wait_pod "$GATEWAY_POD" & pids="$pids $!"; fi
            if [ "${BUILD_SYSTEM}" = "true" ]; then wait_pod "$SYSTEM_POD" & pids="$pids $!"; fi
            if [ "${BUILD_UI}" = "true" ]; then wait_pod "$UI_POD" & pids="$pids $!"; fi
            for pid in $pids; do wait "$pid"; done
          '''
        }
      }
    }

    stage('Render And Apply Helm ConfigMaps To Test') {
      when {
        expression { env.SKIP_PIPELINE != 'true' }
      }
      steps {
        container('helm') {
          sh '''
            set -eu
            helm template aidevops-cloud deploy/helm/aidevops-cloud \
              -n ${K8S_NAMESPACE} \
              -f deploy/helm/aidevops-cloud/values.current-cluster.yaml \
              --show-only templates/configmaps.yaml > /home/jenkins/agent/aidevops-configmaps.yaml
            test -s /home/jenkins/agent/aidevops-configmaps.yaml
          '''
        }
        container('builder') {
          sh '''
            set -eu
            kubectl apply -f /home/jenkins/agent/aidevops-configmaps.yaml
          '''
        }
      }
    }

    stage('Rollout Changed Services To Test') {
      when {
        expression { env.SKIP_PIPELINE != 'true' }
      }
      steps {
        container('builder') {
          sh '''
            set -eu

            if [ "${BUILD_AUTH}" = "true" ]; then
              kubectl -n ${K8S_NAMESPACE} set image deployment/aidevops-auth auth=${REGISTRY}/${TEST_PROJECT}/aidevops-auth:${GIT_COMMIT_TAG}
              kubectl -n ${K8S_NAMESPACE} rollout status deployment/aidevops-auth --timeout=300s
            fi

            if [ "${BUILD_GATEWAY}" = "true" ]; then
              kubectl -n ${K8S_NAMESPACE} set image deployment/aidevops-gateway gateway=${REGISTRY}/${TEST_PROJECT}/aidevops-gateway:${GIT_COMMIT_TAG}
              kubectl -n ${K8S_NAMESPACE} rollout status deployment/aidevops-gateway --timeout=300s
            fi

            if [ "${BUILD_SYSTEM}" = "true" ]; then
              kubectl -n ${K8S_NAMESPACE} set image deployment/aidevops-system system=${REGISTRY}/${TEST_PROJECT}/aidevops-system:${GIT_COMMIT_TAG}
              kubectl -n ${K8S_NAMESPACE} rollout status deployment/aidevops-system --timeout=300s
            fi

            if [ "${BUILD_UI}" = "true" ]; then
              kubectl -n ${K8S_NAMESPACE} set image deployment/aidevops-ui ui=${REGISTRY}/${TEST_PROJECT}/aidevops-ui:${GIT_COMMIT_TAG}
              kubectl -n ${K8S_NAMESPACE} rollout status deployment/aidevops-ui --timeout=300s
            fi
          '''
        }
      }
    }
  }

  post {
    always {
      container('builder') {
        sh '''
          LOCK_NAME=aidevops-shared-release-lock
          LOCK_HOLDER="${JOB_NAME}-${BUILD_NUMBER}"
          CURRENT=$(kubectl -n ${BUILD_NAMESPACE} get configmap "$LOCK_NAME" -o jsonpath='{.data.holder}' 2>/dev/null || true)
          if [ "$CURRENT" = "$LOCK_HOLDER" ]; then
            kubectl -n ${BUILD_NAMESPACE} delete configmap "$LOCK_NAME" --ignore-not-found=true >/dev/null 2>&1 || true
          fi
          kubectl -n ${BUILD_NAMESPACE} delete pod build-auth-${BUILD_NUMBER} build-gateway-${BUILD_NUMBER} build-system-${BUILD_NUMBER} build-ui-${BUILD_NUMBER} --ignore-not-found=true >/dev/null 2>&1 || true
        '''
      }
    }
  }
}
