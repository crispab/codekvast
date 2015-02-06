'use strict';

describe('Controller: MainController', function () {

    // load the controller's module
    beforeEach(module('codekvastApp'));

    var MainController,
        scope;

    // Initialize the controller and a mock scope
    beforeEach(inject(function ($controller, $rootScope) {
        scope = $rootScope.$new();
        MainController = $controller('MainController', {
            $scope: scope
        });
    }));

    it('should have an empty list of signatures', function () {
        expect(scope.signatures.length).toBe(0);
    });

    it('should have an undefined collectorStatus', function () {
        expect(scope.collectorStatus).toBeUndefined();
    });

    it('should have an undefined progress', function () {
        expect(scope.progress).toBeUndefined();
    });

    it('should calculate age', function () {
        var now = Date.now();
        expect(getAge(now, 0)).toBe("");
        expect(getAge(now, now)).toBe("0 min");
        expect(getAge(now, now - 29000)).toBe("0 min");
        expect(getAge(now, now - 30000)).toBe("1 min");
        expect(getAge(now, now - 31000)).toBe("1 min");
        expect(getAge(now, now - 59000)).toBe("1 min");
        expect(getAge(now, now - 61000)).toBe("1 min");
        expect(getAge(now, now - 119000)).toBe("2 min");
        expect(getAge(now, now - 120000)).toBe("2 min");
        expect(getAge(now, now - 121000)).toBe("2 min");
        expect(getAge(now, now - 59 * 60000)).toBe("59 min");
        expect(getAge(now, now - 60 * 60000)).toBe("1 hours");
        expect(getAge(now, now - 24 * 60 * 60000)).toBe("1 days");
        expect(getAge(now, now - 7 * 24 * 60 * 60000)).toBe("1 weeks");
    })
});
