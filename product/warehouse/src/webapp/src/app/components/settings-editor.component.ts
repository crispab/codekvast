import {Component} from '@angular/core';
import {Settings} from './settings.model';
import {StateService} from '../services/state.service';
@Component({
    selector: 'ck-settings-editor',
    template: require('./settings-editor.component.html'),
})
export class SettingsComponent {

    settings: Settings;

    constructor(private stateService: StateService) {
    }

    ngOnInit(): void {
        this.settings = this.stateService.getState(Settings.KEY, () => new Settings());
    }

}
