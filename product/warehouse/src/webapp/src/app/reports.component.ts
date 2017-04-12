import {Component} from '@angular/core';
@Component({
    selector: 'ck-reports',
    template: require('./reports.component.html'),
})
export class ReportsComponent {

    vote: boolean = undefined;

    constructor() {
    }

    doVote(choice: boolean) {
        this.vote = choice;
    }

    hasVoted() {
        return this.vote !== undefined;
    }

    hasVotedFor(choice: boolean) {
        return this.vote === choice;
    }
}
