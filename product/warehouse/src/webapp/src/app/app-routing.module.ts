import {NgModule}             from '@angular/core';
import {RouterModule, Routes} from '@angular/router';
import {Dashboard}            from './dashboard.component';
import {SearchMethods}        from './search-methods.component';

const routes: Routes = [
    {
        path: '',
        redirectTo: '/dashboard',
        pathMatch: 'full'
    },
    {
        path: 'dashboard',
        component: Dashboard
    },
    {
        path: 'search',
        component: SearchMethods
    }
];

@NgModule({
    imports: [RouterModule.forRoot(routes)],
    exports: [RouterModule]
})
export class AppRoutingModule {
}
