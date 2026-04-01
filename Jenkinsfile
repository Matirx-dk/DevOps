pipeline {
  agent {
    kubernetes {
      defaultContainer 'maven'
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
    - name: maven
      image: docker.io/library/maven:3.9.9-eclipse-temurin-17
      imagePullPolicy: IfNotPresent
      command: ["cat"]
      tty: true
      volumeMounts:
        - name: m2-cache
          mountPath: /root/.m2
      resources:
        requests:
          cpu: "500m"
          memory: "1Gi"
        limits:
          cpu: "2"
          memory: "4Gi"
    - name: node
      image: docker.io/library/node:18-alpine
      imagePullPolicy: IfNotPresent
      command: ["sh","-c","cat"]
      tty: true
      volumeMounts:
        - name: npm-cache
          mountPath: /root/.npm
      resources:
        requests:
          cpu: "300m"
          memory: "512Mi"
        limits:
          cpu: "2"
          memory: "2Gi"
  volumes:
    - name: m2-cache
      nfs:
        server: 192.168.1.104
        path: /data/nfs/share/jenkins-cache/m2
    - name: npm-cache
      nfs:
        server: 192.168.1.104
        path: /data/nfs/share/jenkins-cache/npm
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
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
        script {
          sh '''
            git config --global --add safe.directory "$WORKSPACE"
            git config --global --add safe.directory "$(pwd)"
          '''
          env.GIT_COMMIT_SHORT = sh(script: "git rev-parse --short HEAD", returnStdout: true).trim()
          env.GIT_COMMIT_MSG_RAW = sh(script: "git log -1 --pretty=%s", returnStdout: true).trim()
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
            if (f.startsWith('docker/build/') || f == 'Jenkinsfile' || f.startsWith('deploy/k8s/')) {
              targets << 'auth'; targets << 'gateway'; targets << 'system'; targets << 'ui'
            }
          }

          env.BUILD_AUTH = targets.contains('auth') ? 'true' : 'false'
          env.BUILD_GATEWAY = targets.contains('gateway') ? 'true' : 'false'
          env.BUILD_SYSTEM = targets.contains('system') ? 'true' : 'false'
          env.BUILD_UI = targets.contains('ui') ? 'true' : 'false'
          echo "Changed services => auth=${env.BUILD_AUTH}, gateway=${env.BUILD_GATEWAY}, system=${env.BUILD_SYSTEM}, ui=${env.BUILD_UI}"
        }
      }
    }

    stage('Prepare Build Pod Helpers') {
      steps {
        container('maven') {
          sh '''
            KUBECTL_VERSION=v1.28.15
            if ! command -v kubectl >/dev/null 2>&1; then
              curl -fsSL -o /tmp/kubectl https://dl.k8s.io/release/${KUBECTL_VERSION}/bin/linux/amd64/kubectl
              chmod +x /tmp/kubectl
              export PATH=/tmp:$PATH
            fi
          '''
        }
      }
    }

    stage('Build And Push In Dedicated Pods') {
      steps {
        container('maven') {
          sh '''
            set -e
            export PATH=/tmp:$PATH

            wait_pod() {
              POD_NAME="$1"
              while true; do
                PHASE=$(kubectl -n ${BUILD_NAMESPACE} get pod "$POD_NAME" -o jsonpath='{.status.phase}' 2>/dev/null || true)
                if [ "$PHASE" = "Succeeded" ]; then
                  echo "[ok] $POD_NAME succeeded"
                  break
                fi
                if [ "$PHASE" = "Failed" ]; then
                  echo "[fail] $POD_NAME failed"
                  kubectl -n ${BUILD_NAMESPACE} logs "$POD_NAME" --all-containers=true || true
                  return 1
                fi
                sleep 5
              done
            }

            build_backend_pod() {
              POD_NAME="$1"
              MODULE="$2"
              JAR_PATH="$3"
              IMAGE_NAME="$4"
              cat <<YAML | kubectl apply -f -
apiVersion: v1
kind: Pod
metadata:
  name: ${POD_NAME}
  namespace: ${BUILD_NAMESPACE}
  labels:
    app: jenkins-build-pod
spec:
  restartPolicy: Never
  serviceAccountName: jenkins-agent
  tolerations:
    - key: "node-role.kubernetes.io/control-plane"
      operator: "Exists"
      effect: "NoSchedule"
  affinity:
    nodeAffinity:
      preferredDuringSchedulingIgnoredDuringExecution:
        - weight: 100
          preference:
            matchExpressions:
              - key: kubernetes.io/hostname
                operator: In
                values: ["devops-2"]
        - weight: 100
          preference:
            matchExpressions:
              - key: kubernetes.io/hostname
                operator: In
                values: ["devops-4"]
        - weight: 70
          preference:
            matchExpressions:
              - key: kubernetes.io/hostname
                operator: In
                values: ["devops-3"]
        - weight: 30
          preference:
            matchExpressions:
              - key: kubernetes.io/hostname
                operator: In
                values: ["devops-1"]
    podAntiAffinity:
      preferredDuringSchedulingIgnoredDuringExecution:
        - weight: 100
          podAffinityTerm:
            topologyKey: kubernetes.io/hostname
            labelSelector:
              matchExpressions:
                - key: app
                  operator: In
                  values: ["jenkins-build-pod"]
  volumes:
    - name: workspace
      emptyDir: {}
    - name: m2-cache
      nfs:
        server: 192.168.1.104
        path: /data/nfs/share/jenkins-cache/m2
    - name: harbor-config
      secret:
        secretName: harbor-regcred
        items:
          - key: .dockerconfigjson
            path: config.json
  initContainers:
    - name: builder
      image: docker.io/library/maven:3.9.9-eclipse-temurin-17
      command: ["sh","-lc"]
      args:
        - |
          set -e
          git clone --branch ${GIT_BRANCH} --single-branch ${GIT_REPO} /workspace/src
          cd /workspace/src
          mvn -T 1C -DskipTests package -pl ${MODULE} -am
      volumeMounts:
        - name: workspace
          mountPath: /workspace
        - name: m2-cache
          mountPath: /root/.m2
  containers:
    - name: kaniko
      image: gcr.io/kaniko-project/executor:v1.23.2-debug
      args:
        - --context=/workspace/src
        - --dockerfile=/workspace/src/docker/build/backend.Dockerfile
        - --destination=${REGISTRY}/${TEST_PROJECT}/${IMAGE_NAME}:${GIT_COMMIT_TAG}
        - --snapshot-mode=redo
        - --use-new-run
        - --cache=false
        - --skip-tls-verify-registry=${REGISTRY}
        - --build-arg=JAR_PATH=${JAR_PATH}
      volumeMounts:
        - name: workspace
          mountPath: /workspace
        - name: harbor-config
          mountPath: /kaniko/.docker
YAML
            }

            build_ui_pod() {
              POD_NAME="$1"
              cat <<YAML | kubectl apply -f -
apiVersion: v1
kind: Pod
metadata:
  name: ${POD_NAME}
  namespace: ${BUILD_NAMESPACE}
  labels:
    app: jenkins-build-pod
spec:
  restartPolicy: Never
  serviceAccountName: jenkins-agent
  tolerations:
    - key: "node-role.kubernetes.io/control-plane"
      operator: "Exists"
      effect: "NoSchedule"
  affinity:
    nodeAffinity:
      preferredDuringSchedulingIgnoredDuringExecution:
        - weight: 100
          preference:
            matchExpressions:
              - key: kubernetes.io/hostname
                operator: In
                values: ["devops-2"]
        - weight: 100
          preference:
            matchExpressions:
              - key: kubernetes.io/hostname
                operator: In
                values: ["devops-4"]
        - weight: 70
          preference:
            matchExpressions:
              - key: kubernetes.io/hostname
                operator: In
                values: ["devops-3"]
        - weight: 30
          preference:
            matchExpressions:
              - key: kubernetes.io/hostname
                operator: In
                values: ["devops-1"]
    podAntiAffinity:
      preferredDuringSchedulingIgnoredDuringExecution:
        - weight: 100
          podAffinityTerm:
            topologyKey: kubernetes.io/hostname
            labelSelector:
              matchExpressions:
                - key: app
                  operator: In
                  values: ["jenkins-build-pod"]
  volumes:
    - name: workspace
      emptyDir: {}
    - name: npm-cache
      nfs:
        server: 192.168.1.104
        path: /data/nfs/share/jenkins-cache/npm
    - name: harbor-config
      secret:
        secretName: harbor-regcred
        items:
          - key: .dockerconfigjson
            path: config.json
  initContainers:
    - name: builder
      image: docker.io/library/node:18-alpine
      command: ["sh","-lc"]
      args:
        - |
          set -e
          apk add --no-cache git
          git clone --branch ${GIT_BRANCH} --single-branch ${GIT_REPO} /workspace/src
          cd /workspace/src/aidevops-ui
          npm config set registry https://registry.npmmirror.com
          if [ -f package-lock.json ]; then
            npm ci
          else
            npm install
          fi
          npm run build:prod
      volumeMounts:
        - name: workspace
          mountPath: /workspace
        - name: npm-cache
          mountPath: /root/.npm
  containers:
    - name: kaniko
      image: gcr.io/kaniko-project/executor:v1.23.2-debug
      args:
        - --context=/workspace/src
        - --dockerfile=/workspace/src/docker/build/frontend.Dockerfile
        - --destination=${REGISTRY}/${TEST_PROJECT}/aidevops-ui:${GIT_COMMIT_TAG}
        - --snapshot-mode=redo
        - --use-new-run
        - --cache=false
        - --skip-tls-verify-registry=${REGISTRY}
      volumeMounts:
        - name: workspace
          mountPath: /workspace
        - name: harbor-config
          mountPath: /kaniko/.docker
YAML
            }

            cleanup_pod() {
              kubectl -n ${BUILD_NAMESPACE} delete pod "$1" --ignore-not-found=true >/dev/null 2>&1 || true
            }

            AUTH_POD="build-auth-${BUILD_NUMBER}"
            GATEWAY_POD="build-gateway-${BUILD_NUMBER}"
            SYSTEM_POD="build-system-${BUILD_NUMBER}"
            UI_POD="build-ui-${BUILD_NUMBER}"

            cleanup_pod "$AUTH_POD"; cleanup_pod "$GATEWAY_POD"; cleanup_pod "$SYSTEM_POD"; cleanup_pod "$UI_POD"

            pids=""
            if [ "$BUILD_AUTH" = "true" ]; then build_backend_pod "$AUTH_POD" aidevops-auth aidevops-auth/target/aidevops-auth.jar aidevops-auth & pids="$pids $!"; fi
            if [ "$BUILD_GATEWAY" = "true" ]; then build_backend_pod "$GATEWAY_POD" aidevops-gateway aidevops-gateway/target/aidevops-gateway.jar aidevops-gateway & pids="$pids $!"; fi
            for pid in $pids; do wait "$pid"; done

            pids=""
            if [ "$BUILD_SYSTEM" = "true" ]; then build_backend_pod "$SYSTEM_POD" aidevops-modules/aidevops-system aidevops-modules/aidevops-system/target/aidevops-modules-system.jar aidevops-system & pids="$pids $!"; fi
            if [ "$BUILD_UI" = "true" ]; then build_ui_pod "$UI_POD" & pids="$pids $!"; fi
            for pid in $pids; do wait "$pid"; done

            pids=""
            if [ "$BUILD_AUTH" = "true" ]; then wait_pod "$AUTH_POD" & pids="$pids $!"; fi
            if [ "$BUILD_GATEWAY" = "true" ]; then wait_pod "$GATEWAY_POD" & pids="$pids $!"; fi
            for pid in $pids; do wait "$pid"; done

            pids=""
            if [ "$BUILD_SYSTEM" = "true" ]; then wait_pod "$SYSTEM_POD" & pids="$pids $!"; fi
            if [ "$BUILD_UI" = "true" ]; then wait_pod "$UI_POD" & pids="$pids $!"; fi
            for pid in $pids; do wait "$pid"; done
          '''
        }
      }
    }

    stage('Deploy Changed Services To Test') {
      steps {
        container('maven') {
          sh '''
            export PATH=/tmp:$PATH
            if [ "$BUILD_AUTH" = "true" ]; then
              kubectl -n ${K8S_NAMESPACE} set image deployment/aidevops-auth auth=${REGISTRY}/${TEST_PROJECT}/aidevops-auth:${GIT_COMMIT_TAG}
              kubectl -n ${K8S_NAMESPACE} rollout status deployment/aidevops-auth --timeout=300s
            fi

            if [ "$BUILD_GATEWAY" = "true" ]; then
              kubectl -n ${K8S_NAMESPACE} set image deployment/aidevops-gateway gateway=${REGISTRY}/${TEST_PROJECT}/aidevops-gateway:${GIT_COMMIT_TAG}
              kubectl -n ${K8S_NAMESPACE} rollout status deployment/aidevops-gateway --timeout=300s
            fi

            if [ "$BUILD_SYSTEM" = "true" ]; then
              kubectl -n ${K8S_NAMESPACE} set image deployment/aidevops-system system=${REGISTRY}/${TEST_PROJECT}/aidevops-system:${GIT_COMMIT_TAG}
              kubectl -n ${K8S_NAMESPACE} rollout status deployment/aidevops-system --timeout=300s
            fi

            if [ "$BUILD_UI" = "true" ]; then
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
      container('maven') {
        sh '''
          export PATH=/tmp:$PATH
          kubectl -n ${BUILD_NAMESPACE} delete pod build-auth-${BUILD_NUMBER} build-gateway-${BUILD_NUMBER} build-system-${BUILD_NUMBER} build-ui-${BUILD_NUMBER} --ignore-not-found=true >/dev/null 2>&1 || true
        '''
      }
    }
  }
}
