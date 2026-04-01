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
      command:
        - cat
      tty: true
      volumeMounts:
        - name: m2-cache
          mountPath: /root/.m2
      resources:
        requests:
          cpu: \"500m\"
          memory: \"1Gi\"
        limits:
          cpu: \"2\"
          memory: \"4Gi\"
    - name: node
      image: docker.io/library/node:18-alpine
      imagePullPolicy: IfNotPresent
      command:
        - sh
        - -c
        - cat
      tty: true
      volumeMounts:
        - name: npm-cache
          mountPath: /root/.npm
      resources:
        requests:
          cpu: \"300m\"
          memory: \"512Mi\"
        limits:
          cpu: \"2\"
          memory: \"2Gi\"
    - name: kaniko
      image: gcr.io/kaniko-project/executor:v1.23.2-debug
      imagePullPolicy: IfNotPresent
      command:
        - /busybox/sh
        - -c
        - sleep 999999
      tty: true
      volumeMounts:
        - name: kaniko-docker-config
          mountPath: /kaniko/.docker
      resources:
        requests:
          cpu: \"500m\"
          memory: \"1Gi\"
        limits:
          cpu: \"2\"
          memory: \"4Gi\"
  volumes:
    - name: kaniko-docker-config
      secret:
        secretName: harbor-regcred
        items:
          - key: .dockerconfigjson
            path: config.json
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

    stage('Build Backend Jars') {
      when {
        expression {
          env.BUILD_AUTH == 'true' || env.BUILD_GATEWAY == 'true' || env.BUILD_SYSTEM == 'true'
        }
      }
      steps {
        container('maven') {
          sh '''
            mvn -T 1C -DskipTests clean package -pl aidevops-auth,aidevops-gateway,aidevops-modules/aidevops-system -am
          '''
        }
      }
    }

    stage('Build UI') {
      when { expression { env.BUILD_UI == 'true' } }
      steps {
        container('node') {
          sh '''
            cd aidevops-ui
            npm config set registry https://registry.npmmirror.com
            if [ -f package-lock.json ]; then
              npm ci
            else
              npm install
            fi
            npm run build:prod
          '''
        }
      }
    }

    stage('Code Security / Quality Checks') {
      steps {
        container('maven') {
          sh '''
            echo "[check] backend dependency scan skipped temporarily for pipeline bring-up"
          '''
        }
        container('node') {
          sh '''
            set +e
            echo "[check] frontend npm audit"
            if [ -f aidevops-ui/package.json ]; then
              cd aidevops-ui && npm audit --audit-level=high || true
            fi
          '''
        }
      }
    }

    stage('Build Push And Deploy Changed Services') {
      steps {
        container('kaniko') {
          sh '''
            set -e

            if [ "$BUILD_AUTH" = "true" ]; then
              /kaniko/executor \
                --context "$WORKSPACE" \
                --dockerfile "$WORKSPACE/docker/build/backend.Dockerfile" \
                --destination "${REGISTRY}/${TEST_PROJECT}/aidevops-auth:${GIT_COMMIT_TAG}" \
                --snapshot-mode=redo \
                --use-new-run \
                --cache=false \
                --skip-tls-verify-registry "${REGISTRY}" \
                --build-arg JAR_PATH=aidevops-auth/target/aidevops-auth.jar
            fi

            if [ "$BUILD_GATEWAY" = "true" ]; then
              /kaniko/executor \
                --context "$WORKSPACE" \
                --dockerfile "$WORKSPACE/docker/build/backend.Dockerfile" \
                --destination "${REGISTRY}/${TEST_PROJECT}/aidevops-gateway:${GIT_COMMIT_TAG}" \
                --snapshot-mode=redo \
                --use-new-run \
                --cache=false \
                --skip-tls-verify-registry "${REGISTRY}" \
                --build-arg JAR_PATH=aidevops-gateway/target/aidevops-gateway.jar
            fi

            if [ "$BUILD_SYSTEM" = "true" ]; then
              /kaniko/executor \
                --context "$WORKSPACE" \
                --dockerfile "$WORKSPACE/docker/build/backend.Dockerfile" \
                --destination "${REGISTRY}/${TEST_PROJECT}/aidevops-system:${GIT_COMMIT_TAG}" \
                --snapshot-mode=redo \
                --use-new-run \
                --cache=false \
                --skip-tls-verify-registry "${REGISTRY}" \
                --build-arg JAR_PATH=aidevops-modules/aidevops-system/target/aidevops-modules-system.jar
            fi

            if [ "$BUILD_UI" = "true" ]; then
              /kaniko/executor \
                --context "$WORKSPACE" \
                --dockerfile "$WORKSPACE/docker/build/frontend.Dockerfile" \
                --destination "${REGISTRY}/${TEST_PROJECT}/aidevops-ui:${GIT_COMMIT_TAG}" \
                --snapshot-mode=redo \
                --use-new-run \
                --cache=false \
                --skip-tls-verify-registry "${REGISTRY}"
            fi
          '''
        }

        container('maven') {
          sh '''
            KUBECTL_VERSION=v1.28.15
            if ! command -v kubectl >/dev/null 2>&1; then
              curl -fsSL -o /tmp/kubectl https://dl.k8s.io/release/${KUBECTL_VERSION}/bin/linux/amd64/kubectl
              chmod +x /tmp/kubectl
              export PATH=/tmp:$PATH
            fi

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
}
