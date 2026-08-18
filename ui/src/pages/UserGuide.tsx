import { UserGuide } from '@sbb-polarion/react-sbb-polarion';
import useRemote from '../services/useRemote';

/** Feeds the shared User Guide page this extension's REST hook. */
export default function UserGuidePage() {
  const { sendRequest } = useRemote();
  return <UserGuide sendRequest={sendRequest} />;
}
