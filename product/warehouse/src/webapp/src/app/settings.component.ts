import {Component} from '@angular/core';
import {Settings} from './settings';
import {StateService} from './state.service';
@Component({
    selector: 'ck-settings',
    template: require('./settings.component.html'),
})
export class SettingsComponent {

    settings: Settings;

    constructor(private stateService: StateService) {
    }

    ngOnInit(): void {
        this.settings = this.stateService.getState(Settings.KEY, () => new Settings());
    }

}
