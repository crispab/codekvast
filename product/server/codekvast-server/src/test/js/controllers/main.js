'use strict';

describe('Controller: MainCtrl', function () {

    // load the controller's module
    beforeEach(module('codekvastApp'));

    var MainCtrl,
        scope;

    // Initialize the controller and a mock scope
    beforeEach(inject(function ($controller, $rootScope) {
        scope = $rootScope.$new();
        MainCtrl = $controller('MainCtrl', {
            $scope: scope
        });
    }));

    it('should have an empty list of applications', function () {
        expect(scope.applications.length).toBe(0);
    });

    it('should have an undefined application', function () {
        expect(scope.application).toBeUndefined();
    });
});
