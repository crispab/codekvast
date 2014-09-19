var codekvastServices = angular.module('codekvastServices', ['ngResource']);

codekvastServices.factory('Signatures', ['$resource',
    function ($resource) {
        return $resource('user/signatures/', {}, {
            query: {method: 'GET'}
        });
    }]);
