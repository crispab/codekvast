'use strict';

describe('MainController', function () {

    // load the controller's module
    beforeEach(module('codekvastApp'));

    var MainController,
        StompService,
        scope,
        now = Date.now();

    // Initialize the controller and a mock scope
    beforeEach(inject(function ($controller, $rootScope, _StompService_) {
        scope = $rootScope.$new();
        StompService = _StompService_;
        MainController = $controller('MainController', {
            $scope: scope
        });
    }));

    it('should have an undefined list of signatures', function () {
        expect(scope.signatures).toBeUndefined;
    });

    it('should have an undefined collectorStatus', function () {
        expect(scope.collectorStatus).toBeUndefined();
    });

});
