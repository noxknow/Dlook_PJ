import React, { useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { logout, login } from 'app/slices/tokenSlice';
import { styled } from 'styled-components';
import Cookies from 'js-cookie';

import Avatar from '@mui/material/Avatar';
import Box from '@mui/material/Box';
import IconButton from '@mui/material/IconButton';
import Logout from '@mui/icons-material/Logout';
import ListItemIcon from '@mui/material/ListItemIcon';
import Menu from '@mui/material/Menu';
import MenuItem from '@mui/material/MenuItem';
import Tooltip from '@mui/material/Tooltip';

function HeaderUser() {
  const dispatch = useDispatch();
  const isLoggedIn = useSelector((state) => state.members.isLoggedIn);

  const [anchorEl, setAnchorEl] = React.useState(null);
  const open = Boolean(anchorEl);
  const handleClick = (event) => setAnchorEl(event.currentTarget);
  const handleClose = () => setAnchorEl(null);

  const handleLogout = () => {
    Cookies.remove('isLoggedIn');
    Cookies.remove('memberName');
    dispatch(logout());
  };

  useEffect(() => {
    const storedLoginStatus = Cookies.get('isLoggedIn');
    if (storedLoginStatus === 'true') dispatch(login());
  }, [dispatch]);

  const linkTo = isLoggedIn ? '/mypages/certify' : '/members/login';

  return (
    <>
      <li>
        <React.Fragment>
          <Link to={linkTo}>
            <BoxStyle>
              <Tooltip title="내 정보">
                <IconButton
                  onClick={handleClick}
                  sx={{ ml: 2 }}
                  aria-controls={open ? 'account-menu' : undefined}
                  aria-haspopup="true"
                  aria-expanded={open ? 'true' : undefined}
                >
                  <Avatar sx={{ background: 'var(--primary-200)' }}></Avatar>
                </IconButton>
              </Tooltip>
            </BoxStyle>
          </Link>
          {isLoggedIn && (
            <Menu
              anchorEl={anchorEl}
              id="account-menu"
              open={open}
              onClose={handleClose}
              onClick={handleClose}
              PaperProps={{
                elevation: 0,
                sx: {
                  overflow: 'visible',
                  filter: 'drop-shadow(0px 2px 8px rgba(0,0,0,0.32))',
                  mt: 1.5,
                  '& .MuiAvatar-root': {
                    width: 32,
                    height: 32,
                    ml: -0.5,
                    mr: 1,
                  },
                  '&:before': {
                    content: '""',
                    display: 'block',
                    position: 'absolute',
                    top: 0,
                    right: 23,
                    width: 10,
                    height: 10,
                    bgcolor: 'background.paper',
                    transform: 'translateY(-50%) rotate(45deg)',
                    zIndex: 0,
                  },
                },
              }}
              transformOrigin={{ horizontal: 'right', vertical: 'top' }}
              anchorOrigin={{ horizontal: 'right', vertical: 'bottom' }}
            >
              <Link to="/mypages/certify">
                <MenuItem onClick={handleClose}>
                  <Avatar sx={{ background: 'var(--primary-200)' }} />내 정보
                </MenuItem>
              </Link>
              <Link to="/">
                <MenuItem onClick={handleLogout}>
                  <ListItemIcon>
                    <Logout fontSize="small" />
                  </ListItemIcon>
                  Logout
                </MenuItem>
              </Link>
            </Menu>
          )}
        </React.Fragment>
      </li>
    </>
  );
}

const BoxStyle = styled(Box)`
  display: flex;
  align-items: center;
  text-align: center;
`;

export default HeaderUser;
