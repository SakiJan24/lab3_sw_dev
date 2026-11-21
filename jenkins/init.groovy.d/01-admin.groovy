// Crea el usuario administrador en el primer arranque (no hay asistente de instalación).
// Usuario y contraseña llegan por variables de entorno desde jenkins/.env (no versionado).
import jenkins.model.Jenkins
import hudson.security.HudsonPrivateSecurityRealm
import hudson.security.FullControlOnceLoggedInAuthorizationStrategy

def jenkins = Jenkins.get()
def user = System.getenv('JENKINS_ADMIN_USER') ?: 'admin'
def pass = System.getenv('JENKINS_ADMIN_PASSWORD')

if (!(jenkins.securityRealm instanceof HudsonPrivateSecurityRealm)) {
    if (!pass) {
        throw new IllegalStateException('Falta JENKINS_ADMIN_PASSWORD')
    }
    def realm = new HudsonPrivateSecurityRealm(false)
    realm.createAccount(user, pass)
    jenkins.securityRealm = realm

    def strategy = new FullControlOnceLoggedInAuthorizationStrategy()
    strategy.allowAnonymousRead = false
    jenkins.authorizationStrategy = strategy
    jenkins.save()
}
