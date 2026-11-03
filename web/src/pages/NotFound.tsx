import { Link } from 'react-router';
import { Empty, Page } from '../components/ui';

export function NotFound() {
  return (
    <Page title="Page not found">
      <Empty>
        There's nothing here.{' '}
        <Link to="/" className="font-semibold text-blue-ink hover:underline">
          Go to your home screen
        </Link>
      </Empty>
    </Page>
  );
}
