import {Component} from '@angular/core';

@Component({
    selector: 'ck-report-generator',
    template: require('./report-generator.component.html'),
})
export class ReportGeneratorComponent {

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
