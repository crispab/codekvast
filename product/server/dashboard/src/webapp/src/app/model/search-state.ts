export class SearchState {
    static KEY = 'search-state';
    environments = '';
    applications = '';
    hostnames = '';
    includeIfCollectedForAtLeastDays = 30;
    includeIfNotInvokedInDays = 30;
    includeUntrackedMethods = false;
    includeOnlyNeverInvokedMethods = false;

    public isShowingEverything() {
        return this.environments === ''
            && this.applications === ''
            && this.hostnames === ''
            && this.includeIfNotInvokedInDays === 0
            && !this.includeOnlyNeverInvokedMethods;
    }
}
