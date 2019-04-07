import {Component, OnInit} from '@angular/core';
import {ClientSettings} from '../../model/client-settings';
import {StateService} from '../../services/state.service';

@Component({
    selector: 'app-settings-editor',
    templateUrl: './settings-editor.component.html'
})
export class SettingsEditorComponent implements OnInit {

    settings: ClientSettings;

    constructor(private stateService: StateService) {
    }

    ngOnInit(): void {
        this.settings = this.stateService.getState(ClientSettings.KEY, () => new ClientSettings());
    }

}
