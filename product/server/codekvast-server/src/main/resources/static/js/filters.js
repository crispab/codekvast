var codekvastFilters = angular.module('codekvastFilters', []);

codekvastFilters.filter('suppressEmptyDate', function () {
    return function (input) {
        return input == 0 ? "" : input;
    };
});
