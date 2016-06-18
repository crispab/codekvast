import {describe, inject, beforeEach, beforeEachProviders} from '@angular/core/testing';
import {TestComponentBuilder} from '@angular/compiler/testing';
import {MethodListComponent} from './method-list.component';

describe('MethodListComponent', () => {

    // TODO: make test work

    let tcb;

    //setup
    beforeEachProviders(() => [TestComponentBuilder, MethodListComponent]);

    beforeEach(inject([TestComponentBuilder], _tcb => {
        tcb = _tcb
    }));

    xit('should render nothing for zero-valued timestamps', done => {
        tcb.createAsync(MethodListComponent).then(fixture => {
               let methodList = fixture.componentInstance, element = fixture.nativeElement;
               methodList.data = {
                   methods: [
                       {
                           signature: 'sig1',
                           lastInvokedAtMillis: 0,
                           collectedSinceMillis: 0,
                           collectedToMillis: 0,
                           collectedDays: 0
                       }

                   ]
               };
               fixture.detectChanges(); //trigger change detection
               expect(element.querySelector('h1').innerText).toBe('Hello World!');
               done();
           })
           .catch(e => done.fail(e));
    });
});
