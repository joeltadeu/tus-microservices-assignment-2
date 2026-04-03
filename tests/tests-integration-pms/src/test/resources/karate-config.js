function fn() {
    let config = {};

    karate.configure('connectTimeout', 120000)
    karate.configure('readTimeout', 120000)
    karate.configure('headers', { 'Accept-Encoding': null })


    let env = karate.env ? karate.env : 'local';
    config.env = env

    config.gatewayHost = 'http://localhost:9094'
    config.patientHost = 'http://localhost:9094'
    config.doctorHost = 'http://localhost:9094'
    config.appointmentHost = 'http://localhost:9094'
    config.authHost = 'http://localhost:9094'

    console.log('Gateway URL: ', config.gatewayHost);

    config.config = read('classpath:com/pms/integration/features/_config.json')

    return config;
}