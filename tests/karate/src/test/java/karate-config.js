function fn() {
    var port = karate.properties['server.port'] || '8081';
    var baseUrl = karate.properties['baseUrl'] || 'http://localhost:' + port;
    var config = {
        baseUrl: baseUrl,
        authUrl: baseUrl + '/api/v1/auth',
        usersUrl: baseUrl + '/api/v1/users',
        productsUrl: baseUrl + '/api/v1/products',
        categoriesUrl: baseUrl + '/api/v1/categories',
        suppliersUrl: baseUrl + '/api/v1/suppliers'
    };
    karate.configure('connectTimeout', 10000);
    karate.configure('readTimeout', 10000);

    var authResult = karate.callSingle('classpath:karate/setup-and-auth.feature', config);
    config.adminToken = authResult.adminToken;
    karate.configure('headers', { Authorization: 'Bearer ' + config.adminToken });

    return config;
}
