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
      image: harbor.zoudekang.cloud/dockerhub-proxy/library/maven:3.9.9-eclipse-temurin-17
      imagePullPolicy: IfNotPresent
      command: [\"cat\"]
      tty: true
      volumeMounts:
        - name: m2-cache
          mountPath: /root/.m2
    - name: node
      image: harbor.zoudekang.cloud/dockerhub-proxy/library/node:18-alpine
      imagePullPolicy: IfNotPresent
      command: [\"sh\",\"-c\",\"cat\"]
      tty: true
      volumeMounts:
        - name: npm-cache
          mountPath: /root/.npm
    - name: kaniko
      image: harbor.zoudekang.cloud/ci-tools/kaniko-executor:v1.23.2-debug
      imagePullPolicy: IfNotPresent
      command: [\"sh\",\"-c\",\"cat\"]
      tty: true
      volumeMounts:
        - name: harbor-config
          mountPath: /kaniko/.docker
    - name: helm
      image: harbor.zoudekang.cloud/ci-tools/helm:3.16.4
      imagePullPolicy: IfNotPresent
      command: [\"cat\"]
      tty: true
  volumes:
    - name: m2-cache
      nfs:
        server: 192.168.1.104
        path: /data/nfs/share/jenkins-cache/m2
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
        stash name: 'src', includes: '**/*', useDefaultExcludes: false
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
            if (f.startsWith('docker/build/') || f == 'Jenkinsfile' || f.startsWith('deploy/')) {
              targets << 'auth'; targets << 'gateway'; targets << 'system'; targets << 'ui'
            }
          }

          def anyChange = !targets.isEmpty()
          env.SKIP_PIPELINE = anyChange ? 'false' : 'true'
          if (anyChange) {
            env.BUILD_AUTH = 'true'
            env.BUILD_GATEWAY = 'true'
            env.BUILD_SYSTEM = 'true'
            env.BUILD_UI = 'true'
            echo "Detected changes in ${targets}. Build and deploy all core services together for version alignment."
          } else {
            env.BUILD_AUTH = 'false'
            env.BUILD_GATEWAY = 'false'
            env.BUILD_SYSTEM = 'false'
            env.BUILD_UI = 'false'
            currentBuild.description = 'No relevant changes detected'
          }
          echo "Build plan => auth=${env.BUILD_AUTH}, gateway=${env.BUILD_GATEWAY}, system=${env.BUILD_SYSTEM}, ui=${env.BUILD_UI}"
        }
      }
    }

    stage('Build And Push Images') {
      when {
        expression { env.SKIP_PIPELINE != 'true' }
      }
      parallel {
        stage('Build Auth') {
          when { expression { env.BUILD_AUTH == 'true' } }
          steps {
            ws("${env.WORKSPACE}@auth") {
              deleteDir()
              unstash 'src'
              container('maven') {
                sh '''
                  mvn -Dmaven.repo.local=$PWD/.m2/repository -T 1C -DskipTests package -pl aidevops-auth -am
                '''
              }
              container('kaniko') {
                sh '''
                  /kaniko/executor \
                    --context="$PWD" \
                    --dockerfile="$PWD/docker/build/backend.Dockerfile" \
                    --destination=${REGISTRY}/${TEST_PROJECT}/aidevops-auth:${GIT_COMMIT_TAG} \
                    --snapshot-mode=redo \
                    --use-new-run \
                    --cache=false \
                    --skip-tls-verify-registry=${REGISTRY} \
                    --build-arg=JAR_PATH=aidevops-auth/target/aidevops-auth.jar
                '''
              }
            }
          }
        }

        stage('Build Gateway') {
          when { expression { env.BUILD_GATEWAY == 'true' } }
          steps {
            ws("${env.WORKSPACE}@gateway") {
              deleteDir()
              unstash 'src'
              container('maven') {
                sh '''
                  mvn -Dmaven.repo.local=$PWD/.m2/repository -T 1C -DskipTests package -pl aidevops-gateway -am
                '''
              }
              container('kaniko') {
                sh '''
                  /kaniko/executor \
                    --context="$PWD" \
                    --dockerfile="$PWD/docker/build/backend.Dockerfile" \
                    --destination=${REGISTRY}/${TEST_PROJECT}/aidevops-gateway:${GIT_COMMIT_TAG} \
                    --snapshot-mode=redo \
                    --use-new-run \
                    --cache=false \
                    --skip-tls-verify-registry=${REGISTRY} \
                    --build-arg=JAR_PATH=aidevops-gateway/target/aidevops-gateway.jar
                '''
              }
            }
          }
        }

        stage('Build System') {
          when { expression { env.BUILD_SYSTEM == 'true' } }
          steps {
            ws("${env.WORKSPACE}@system") {
              deleteDir()
              unstash 'src'
              container('maven') {
                sh '''
                  mvn -Dmaven.repo.local=$PWD/.m2/repository -T 1C -DskipTests package -pl aidevops-modules/aidevops-system -am
                '''
              }
              container('kaniko') {
                sh '''
                  /kaniko/executor \
                    --context="$PWD" \
                    --dockerfile="$PWD/docker/build/backend.Dockerfile" \
                    --destination=${REGISTRY}/${TEST_PROJECT}/aidevops-system:${GIT_COMMIT_TAG} \
                    --snapshot-mode=redo \
                    --use-new-run \
                    --cache=false \
                    --skip-tls-verify-registry=${REGISTRY} \
                    --build-arg=JAR_PATH=aidevops-modules/aidevops-system/target/aidevops-modules-system.jar
                '''
              }
            }
          }
        }

        stage('Build UI') {
          when { expression { env.BUILD_UI == 'true' } }
          steps {
            ws("${env.WORKSPACE}@ui") {
              deleteDir()
              unstash 'src'
              container('node') {
                sh '''
                  cd aidevops-ui
                  npm config set registry https://registry.npmmirror.com
                  npm config set cache "$PWD/.npm-cache" --global
                  if [ -f package-lock.json ]; then
                    npm ci
                  else
                    npm install
                  fi
                  npm run build:prod
                '''
              }
              container('kaniko') {
                sh '''
                  /kaniko/executor \
                    --context="$PWD" \
                    --dockerfile="$PWD/docker/build/frontend.Dockerfile" \
                    --destination=${REGISTRY}/${TEST_PROJECT}/aidevops-ui:${GIT_COMMIT_TAG} \
                    --snapshot-mode=redo \
                    --use-new-run \
                    --cache=false \
                    --skip-tls-verify-registry=${REGISTRY}
                '''
              }
            }
          }
        }
      }
    }

    stage('Deploy Changed Services To Test') {
      when {
        expression { env.SKIP_PIPELINE != 'true' }
      }
      steps {
        ws("${env.WORKSPACE}@deploy") {
          deleteDir()
          unstash 'src'
          container('helm') {
            sh '''
              helm upgrade --install aidevops-test deploy/helm/aidevops-cloud \
                -n ${K8S_NAMESPACE} \
                --create-namespace \
                -f deploy/helm/aidevops-cloud/values.test.yaml \
                --set auth.image=${REGISTRY}/${TEST_PROJECT}/aidevops-auth:${GIT_COMMIT_TAG} \
                --set gateway.image=${REGISTRY}/${TEST_PROJECT}/aidevops-gateway:${GIT_COMMIT_TAG} \
                --set system.image=${REGISTRY}/${TEST_PROJECT}/aidevops-system:${GIT_COMMIT_TAG} \
                --set ui.image=${REGISTRY}/${TEST_PROJECT}/aidevops-ui:${GIT_COMMIT_TAG} \
                --wait \
                --timeout 10m
            '''
          }
        }
      }
    }
  }
}
